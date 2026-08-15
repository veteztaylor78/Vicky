<?php
require_once '../config/database.php';

$message = "";

if ($_SERVER["REQUEST_METHOD"] === "POST") {

    $full_name = trim($_POST['full_name']);
    $username  = trim($_POST['username']);
    $email     = trim($_POST['email']);
    $password  = $_POST['password'];
    $confirm   = $_POST['confirm_password'];

    if ($password !== $confirm) {

        $message = "Passwords do not match!";

    } else {

        // Check if username or email already exists
        $check = $pdo->prepare(
            "SELECT id FROM users WHERE email = ? OR username = ?"
        );

        $check->execute([$email, $username]);

        if ($check->fetch()) {

            $message = "Username or Email already exists.";

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

                header("Location: login.php?registered=1");
                exit;

            } else {

                $message = "Registration failed.";

            }
        }
    }
}
?>