# PayWay — Frontend Integration Guide

How the subscription screens call the backend. API details and full responses: [PAYWAY_INTEGRATION.md](PAYWAY_INTEGRATION.md). Postman: `postman/PayWay_Frontend.postman_collection.json`.

---

## 1. What you need

| Item | Where from |
|---|---|
| Logged-in lawyer's `accessToken` | `POST /api/auth/login` (existing) |
| PayWay **publishable** key | Frontend config (e.g. `VITE_PAYWAY_PUBLISHABLE_KEY`). Sandbox keys start with `T…_PUB_`. It is safe in the browser; it can only create card tokens. **Never** put the secret key in the frontend. |
| Plans (`id`, `plan_name`, `monthly_price`, `annual_price`, `features`) | The `subscriptions` table. The `id` and prices you send must match it. |

---

## 2. Screens → API calls

| Screen | Call |
|---|---|
| Subscriptions (plan cards, Monthly / Annually toggle) | Plans data. On **Select Plan** keep `planId`, `planDuration` (`monthly` / `yearly`) and the price. |
| Checkout (card form, "Pay $X and Subscribe") | `payway.js` card frame → `singleUseTokenId`, then `POST /api/payway/payments` |
| Processing | While the payment call is in flight (can take ~20 s if the bank is slow). Disable the Pay button. |
| Success | Payment returned `200` |
| Payment failed | Payment returned `402`, `422` or `502` — show `message`, render a **new** card frame to retry |
| Billing History | `GET /api/payway/invoices` |

Show these screens to lawyers only. A lawyer who joined a firm (not the firm admin) gets `403` — show "Your firm's subscription is managed by your firm admin".

---

## 3. Checkout amounts

```js
const amount = planDuration === "yearly" ? plan.annual_price : plan.monthly_price; // exactly as stored
const gstAmount = Math.round(amount * 10) / 100;                                    // 10% GST, 2 decimals
const totalAmount = Math.round((amount + gstAmount) * 100) / 100;                   // what is charged
```

The server rejects the payment (`400`) if `amount` isn't the plan's price for that duration, or if `totalAmount ≠ amount + gstAmount` to the cent.

---

## 4. Card form (payway.js)

PayWay renders the card fields (cardholder name, number, expiry, CVN) inside its own iframe, so card details never touch our pages or servers. You get back a `singleUseTokenId`. The browser can't call PayWay's REST API directly (no CORS) — always use the frame. Based on [PayWay's trusted frame example](https://www.payway.com.au/docs/rest.html#payway-js).

```html
<div id="payway-credit-card"></div>
<button id="pay" disabled>Pay $22.00 and Subscribe</button>
<script src="https://api.payway.com.au/rest/v1/payway.js"></script>
```

```js
const payButton = document.getElementById("pay");
let creditCardFrame = null;

function renderCardFrame() {
  payway.createCreditCardFrame(
    {
      publishableApiKey: PAYWAY_PUBLISHABLE_KEY,
      tokenMode: "callback",
      onValid: () => { payButton.disabled = false; },
      onInvalid: () => { payButton.disabled = true; },
    },
    (err, frame) => {
      if (err) { showError("Could not load the card form: " + err.message); return; }
      creditCardFrame = frame;
    }
  );
}

payButton.onclick = () => {
  payButton.disabled = true;               // no double submit
  showProcessing();

  creditCardFrame.getToken(async (err, data) => {
    // A token works once: drop this frame whatever happens
    creditCardFrame.destroy();
    creditCardFrame = null;

    if (err) { showFailed(err.message); renderCardFrame(); return; }

    const res = await fetch(`${API_BASE}/api/payway/payments`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${accessToken}` },
      body: JSON.stringify({
        planId, planDuration, amount, gstAmount, totalAmount,
        singleUseTokenId: data.singleUseTokenId,
      }),
    });
    const body = await res.json();

    if (res.status === 200) {
      showSuccess(body.data);               // body.data.renewsAt = next billing date
    } else {
      showFailed(body.message);             // e.g. "Payment declined: Not sufficient funds"
      if (res.status !== 409 && res.status !== 403) renderCardFrame();
    }
  });
};

renderCardFrame();
```

---

## 5. Handling the payment response

| HTTP | Meaning | What to show / do |
|---|---|---|
| 200 | Approved. Subscription active. | Success screen. Useful fields: `data.invoiceNumber`, `data.receiptNumber`, `data.totalAmount`, `data.maskedCardNumber`, `data.renewsAt` (next billing date) |
| 402 | Card declined by the bank | Payment failed: `message` (e.g. "Payment declined: Not sufficient funds"). New card frame, let them retry. |
| 422 | Token or card rejected by PayWay (expired / reused token, bad card) | Payment failed: `message`. New card frame. |
| 400 | Wrong amounts / missing fields | A frontend bug — check the amounts and request body. |
| 403 | Firm member | "Your firm's subscription is managed by your firm admin" |
| 409 | Already subscribed, or a payment already in progress | Go to the subscription page; don't retry automatically. |
| 502 | PayWay unavailable / still processing | "Something went wrong, please try again shortly". Don't retry immediately. |
| 503 | Payments not configured on this environment | Show a generic error. |

Rules:
- **One token per attempt.** Tokens are single-use and expire after 10 minutes — after any failure, destroy the frame and render a new one.
- **Never** send card numbers to our API or build your own card inputs.
- Retrying after a decline with the same plan reuses the same invoice, so billing history doesn't get duplicates.

---

## 6. Billing History screen

`GET /api/payway/invoices` → `data` is a list, newest first.

| Column | Field | Example |
|---|---|---|
| Date | `date` | `2026-10-04` → "4 Oct 2026" |
| Description | `description` | "Subscription for Individual Lawyers — Monthly" |
| Amount | `totalAmount` | `22.0` → "$22.00" |
| Status | `status` | `PAID` → "Paid", `OVERDUE` → "Overdue", `DUE` → "Due" |
| Download | `invoiceNumber`, `receiptNumber` | PDF endpoint not built yet |

Empty list (`"data": []`) = no invoices yet. Dates are plain dates without a timezone; amounts are dollars.

---

## 7. Testing

Sandbox test cards (any cardholder name of 4+ characters):

| Card | CVN | Expiry | Result |
|---|---|---|---|
| `4564710000000004` | 847 | 02/29 | Approved |
| `5163200000000008` | 070 | 08/30 | Approved |
| `4564710000000020` | 234 | 05/30 | Declined — "Not sufficient funds" (easiest way to see the failed screen) |

Postman (`PayWay_Frontend` collection) runs the same flow without a browser: login → card token → make payment → billing history.
