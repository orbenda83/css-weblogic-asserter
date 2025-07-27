<!DOCTYPE html>
<html>
<head>
    <title>Protected Page</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 50px; }
        .container { width: 600px; padding: 20px; border: 1px solid #007bff; border-radius: 5px; margin: auto; background-color: #e6f7ff; }
        h2 { color: #0056b3; }
        p { line-height: 1.6; }
        a { color: #cc0000; text-decoration: none; }
        a:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <div class="container">
        <h2>Welcome to the Protected Area!</h2>
        <p>You have successfully accessed a secured resource.</p>
        <p>This means that either your form-based login was successful, or your WebLogic Custom Asserter correctly validated your identity (e.g., via the "OAS_USER" header).</p>
        <p>
            To test your custom asserter:
            <ol>
                <li>Ensure your custom asserter is deployed and configured in your WebLogic security realm.</li>
                <li>Make sure it's set up to validate the "OAS_USER" header.</li>
                <li>Try accessing <code>/WebAppTester/protected/index.jsp</code> directly.</li>
                <li>If the asserter works, you should land on this page without seeing the login form (assuming the "OAS_USER" header is present and valid in your request).</li>
                <li>If not, you'll be redirected to <code>login.jsp</code>.</li>
            </ol>
        </p>
        <p><a href="j_security_check?logout=true">Logout</a></p>
    </div>
</body>
</html>