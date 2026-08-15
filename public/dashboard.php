<?php
session_start();

if (!isset($_SESSION['user_id'])) {
    header("Location: login.php");
    exit;
}

$fullName = $_SESSION['full_name'];
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard | AI Vet Tech</title>
    <link rel="stylesheet" href="../assets/css/style.css">
</head>
<body>

<div class="dashboard-container">

    <h1>Welcome, <?= htmlspecialchars($fullName) ?> 👋</h1>

    <p>Welcome to AI Vet Tech.</p>

    <div class="tools">

        <a href="#">🎨 AI Images</a><br><br>

        <a href="#">👤 AI Characters</a><br><br>

        <a href="#">🎬 Image to Video</a><br><br>

        <a href="#">🎙️ Text to Voice</a><br><br>

        <a href="#">🌍 Translator</a><br><br>

        <a href="#">📂 My Projects</a><br><br>

    </div>

    <p>

        <a href="logout.php">Logout</a>

    </p>

</div>

</body>
</html>