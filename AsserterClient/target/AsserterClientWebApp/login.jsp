<!DOCTYPE html>
<html>
<head>
    <title>Login</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 50px; }
        .container { width: 300px; padding: 20px; border: 1px solid #ccc; border-radius: 5px; margin: auto; }
        input[type="text"], input[type="password"] { width: 100%; padding: 8px; margin: 8px 0; display: inline-block; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
        input[type="submit"] { background-color: #4CAF50; color: white; padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; width: 100%; }
        input[type="submit"]:hover { background-color: #45a049; }
    </style>
</head>
<body>
    <div class="container">
        <h2>Login to WebLogic Asserter Tester</h2>
        <form action="j_security_check" method="POST">
            <label for="username">Username:</label>
            <input type="text" id="username" name="j_username" required><br>

            <label for="password">Password:</label>
            <input type="password" id="password" name="j_password" required><br>

            <input type="submit" value="Login">
        </form>
        <p>
            This application is protected. If a custom asserter is configured
            and successfully asserts your identity (e.g., from a custom header),
            you should be automatically logged in without seeing this page.
            <br><br>
            If not, or if using basic form-based auth, use `j_username` and `j_password`
            fields for traditional login.
        </p>
    </div>
</body>
</html>