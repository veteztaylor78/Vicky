<?php

$host = "sql208.infinityfree.com";
$dbname = "if0_42503845_ai_vet_tech";
$username = "if0_42503845";
$password = "Greatness198010"; // Replace with your real MySQL password

try {
    $pdo = new PDO(
        "mysql:host=$host;dbname=$dbname;charset=utf8mb4",
        $username,
        $password
    );

    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

} catch (PDOException $e) {
    die("Database connection failed: " . $e->getMessage());
}