window.onload = function() {
  const checkInterval = setInterval(() => {
    const topbar = document.querySelector(".topbar");
    if (topbar) {
      clearInterval(checkInterval);

      // ✅ Create the Login button
      const loginButton = document.createElement("button");
      loginButton.textContent = "🔐 Login";
      loginButton.style = `
        background-color: #2e7d32;
        color: white;
        border: none;
        padding: 6px 12px;
        border-radius: 6px;
        cursor: pointer;
        margin-left: 10px;
      `;

      loginButton.onclick = async () => {
        const email = prompt("Enter email:");
        const password = prompt("Enter password:");
        if (!email || !password) return;

        try {
          const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password }),
          });

          if (!response.ok) {
            alert("Login failed: " + response.statusText);
            return;
          }

          const data = await response.json();

          // ✅ Your token path: data.accessToken inside "data" object
          const token = data?.data?.accessToken;
          const uuid = data?.data?.uuid;

          if (!token) {
            alert("No accessToken found in response!");
            console.log("Response data:", data);
            return;
          }

          // ✅ Set token in Swagger UI
          window.ui.preauthorizeApiKey("bearerAuth", "Bearer " + token);

          // ✅ (Optional) Log UUID for debugging
          console.log("UUID:", uuid);

          alert("✅ Logged in! Token stored for Swagger requests.");
        } catch (err) {
          alert("Error during login: " + err.message);
        }
      };

      topbar.appendChild(loginButton);
    }
  }, 1000);
};
