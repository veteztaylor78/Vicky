<?php
require_once '../config/database.php';

$message = "";

if ($_SERVER["REQUEST_METHOD"] == "POST") {

    $full_name = trim($_POST['full_name']);
    $username  = trim($_POST['username']);
    $email     = trim($_POST['email']);
    $password  = $_POST['password'];
    $confirm   = $_POST['confirm_password'];

    if ($password !== $confirm) {
        $message = "Passwords do not match!";
    } else {

        $passwordHash = password_hash($password, PASSWORD_DEFAULT);

        $sql = "INSERT INTO users(full_name, username, email, password)
                VALUES(?,?,?,?)";

        $stmt = $pdo->prepare($sql);

        if ($stmt->execute([
            $full_name,
            $username,
            $email,
            $passwordHash
        ])) {

            $message = "Account created successfully!";
        } else {

            $message = "Registration failed.";

        }
    }
}
?>

<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="UTF-8">

<meta name="viewport" content="width=device-width, initial-scale=1.0">

<title>Create Account</title>

<link rel="stylesheet" href="../assets/css/style.css">

</head>

<body>

<div class="register-container">

<h1>AI Vet Tech</h1>

<h2>Create Your Account</h2>

<?php if($message!=""): ?>

<p><?php echo $message; ?></p>

<?php endif; ?>

<form method="POST">

<input
type="text"
name="full_name"
placeholder="Full Name"
required>

<input
type="text"
name="username"
placeholder="Username"
required>

<input
type="email"
name="email"
placeholder="Email Address"
required>

<input
type="password"
name="password"
placeholder="Password"
required>

<input
type="password"
name="confirm_password"
placeholder="Confirm Password"
required>

<button type="submit">

Create Account

</button>

</form>

<p>

Already have an account?

<a href="login.php">

Login

</a>

</p>

</div>

</body>

</html>