<?php

session_start();

error_reporting(E_ALL);
ini_set('display_errors', '1');

$message = "";
$messageType = "";

// Database connection
try {
    require_once __DIR__ . '/config/database.php';
} catch (Throwable $e) {
    die("Database configuration error: " . htmlspecialchars($e->getMessage()));
}


// Handle login
if ($_SERVER["REQUEST_METHOD"] === "POST") {

    $login = trim($_POST["login"] ?? "");
    $password = $_POST["password"] ?? "";

    if ($login === "" || $password === "") {

        $message = "Please enter your username/email and password.";
        $messageType = "error";

    } else {

        try {

            // Find user by username OR email
            $sql = "SELECT id, full_name, username, email, password
                    FROM users
                    WHERE username = ? OR email = ?
                    LIMIT 1";

            $stmt = $pdo->prepare($sql);
            $stmt->execute([$login, $login]);

            $user = $stmt->fetch(PDO::FETCH_ASSOC);

            if ($user && password_verify($password, $user["password"])) {

                // Login successful
                $_SESSION["user_id"] = $user["id"];
                $_SESSION["full_name"] = $user["full_name"];
                $_SESSION["username"] = $user["username"];
                $_SESSION["email"] = $user["email"];

                // Go to dashboard
                header("Location: dashboard.php");
                exit;

            } else {

                $message = "Incorrect username/email or password.";
                $messageType = "error";

            }

        } catch (PDOException $e) {

            $message = "Database error: " . $e->getMessage();
            $messageType = "error";
        }
    }
}

?>

<!DOCTYPE html>
<html lang="en">

<head>

    <meta charset="UTF-8">

    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>Login - AI Vet Tech</title>

    <style>

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            padding: 0;
            min-height: 100vh;
            font-family: Arial, Helvetica, sans-serif;
            background: linear-gradient(135deg, #0f766e, #164e63);
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .login-container {
            width: 100%;
            max-width: 420px;
            background: white;
            padding: 40px;
            border-radius: 18px;
            box-shadow: 0 15px 40px rgba(0,0,0,0.2);
        }

        .logo {
            text-align: center;
            margin-bottom: 25px;
        }

        .logo h1 {
            margin: 0;
            color: #0f766e;
            font-size: 32px;
        }

        .logo p {
            margin-top: 8px;
            color: #666;
        }

        h2 {
            text-align: center;
            margin-bottom: 25px;
            color: #222;
        }

        .message {
            padding: 12px;
            margin-bottom: 20px;
            border-radius: 8px;
            background: #fee2e2;
            color: #991b1b;
            text-align: center;
        }

        .form-group {
            margin-bottom: 18px;
        }

        label {
            display: block;
            margin-bottom: 7px;
            font-weight: bold;
            color: #333;
        }

        input {
            width: 100%;
            padding: 13px;
            border: 1px solid #ccc;
            border-radius: 8px;
            font-size: 16px;
            outline: none;
        }

        input:focus {
            border-color: #0f766e;
        }

        button {
            width: 100%;
            padding: 14px;
            border: none;
            border-radius: 8px;
            background: #0f766e;
            color: white;
            font-size: 17px;
            font-weight: bold;
            cursor: pointer;
        }

        button:hover {
            background: #115e59;
        }

        .register-link {
            text-align: center;
            margin-top: 22px;
            color: #555;
        }

        .register-link a {
            color: #0f766e;
            font-weight: bold;
            text-decoration: none;
        }

        .back-home {
            text-align: center;
            margin-top: 15px;
        }

        .back-home a {
            color: #666;
            text-decoration: none;
        }

    </style>

</head>

<body>

<div class="login-container">

    <div class="logo">

        <h1>AI Vet Tech</h1>

        <p>Smart Veterinary Technology</p>

    </div>

    <h2>Welcome Back</h2>

    <?php if ($message !== ""): ?>

        <div class="message">
            <?php echo htmlspecialchars($message); ?>
        </div>

    <?php endif; ?>

    <form method="POST" action="login.php">

        <div class="form-group">

            <label for="login">
                Username or Email
            </label>

            <input
                type="text"
                id="login"
                name="login"
                placeholder="Enter username or email"
                required
                autocomplete="username"
            >

        </div>


        <div class="form-group">

            <label for="password">
                Password
            </label>

            <input
                type="password"
                id="password"
                name="password"
                placeholder="Enter your password"
                required
                autocomplete="current-password"
            >

        </div>


        <button type="submit">
            Login
        </button>

    </form>


    <div class="register-link">

        Don't have an account?

        <a href="register.php">
            Create Account
        </a>

    </div>


    <div class="back-home">

        <a href="index.php">
            ← Back to Home
        </a>

    </div>

</div>

</body>

</html>
