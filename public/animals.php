<?php

error_reporting(E_ALL);
ini_set('display_errors', 1);

session_start();

/*
|--------------------------------------------------------------------------
| Database connection
|--------------------------------------------------------------------------
*/

$dbFile = __DIR__ . '/../config/database.php';

if (!file_exists($dbFile)) {
    die("ERROR: database.php was not found at: " . $dbFile);
}

require_once $dbFile;


/*
|--------------------------------------------------------------------------
| Check login
|--------------------------------------------------------------------------
*/

if (!isset($_SESSION['user_id'])) {
    header("Location: login.php");
    exit;
}

$user_id = (int) $_SESSION['user_id'];

$message = "";
$message_type = "";


/*
|--------------------------------------------------------------------------
| Add animal
|--------------------------------------------------------------------------
*/

if ($_SERVER["REQUEST_METHOD"] === "POST") {

    $animal_name      = trim($_POST['animal_name'] ?? '');
    $species          = trim($_POST['species'] ?? '');
    $breed            = trim($_POST['breed'] ?? '');
    $sex              = trim($_POST['sex'] ?? '');
    $age              = trim($_POST['age'] ?? '');
    $owner_name       = trim($_POST['owner_name'] ?? '');
    $medical_history  = trim($_POST['medical_history'] ?? '');
    $notes            = trim($_POST['notes'] ?? '');

    if ($animal_name === '' || $species === '') {

        $message = "Animal name and species are required.";
        $message_type = "error";

    } else {

        try {

            $sql = "
                INSERT INTO animals
                (
                    user_id,
                    animal_name,
                    species,
                    breed,
                    sex,
                    age,
                    owner_name,
                    medical_history,
                    notes
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ";

            $stmt = $pdo->prepare($sql);

            $stmt->execute([
                $user_id,
                $animal_name,
                $species,
                $breed,
                $sex,
                $age,
                $owner_name,
                $medical_history,
                $notes
            ]);

            $message = "Animal record added successfully!";
            $message_type = "success";

        } catch (PDOException $e) {

            $message = "Could not save the animal record.";
            $message_type = "error";
        }
    }
}


/*
|--------------------------------------------------------------------------
| Get user's animals
|--------------------------------------------------------------------------
*/

try {

    $stmt = $pdo->prepare("
        SELECT
            id,
            animal_name,
            species,
            breed,
            sex,
            age,
            owner_name,
            medical_history,
            notes,
            created_at
        FROM animals
        WHERE user_id = ?
        ORDER BY id DESC
    ");

    $stmt->execute([$user_id]);

    $animals = $stmt->fetchAll(PDO::FETCH_ASSOC);

} catch (PDOException $e) {

    die("ERROR loading animal records: " . htmlspecialchars($e->getMessage()));
}

?>

<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="UTF-8">

<meta name="viewport" content="width=device-width, initial-scale=1.0">

<title>Animal Records - AI Vet Tech</title>

<style>

* {
    box-sizing: border-box;
}

body {
    margin: 0;
    font-family: Arial, Helvetica, sans-serif;
    background: #f4f7f9;
    color: #172033;
}

.header {
    background: #117c75;
    color: white;
    padding: 20px 30px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.header h1 {
    margin: 0;
    font-size: 26px;
}

.header a {
    color: white;
    text-decoration: none;
    font-weight: bold;
}

.container {
    max-width: 1200px;
    margin: 30px auto;
    padding: 0 20px;
}

.page-title {
    margin-bottom: 25px;
}

.page-title h2 {
    margin: 0 0 8px;
    font-size: 30px;
}

.page-title p {
    margin: 0;
    color: #65748b;
}

.card {
    background: white;
    border-radius: 14px;
    padding: 25px;
    margin-bottom: 25px;
    box-shadow: 0 3px 15px rgba(0,0,0,0.06);
}

.card h3 {
    margin-top: 0;
    font-size: 22px;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
}

.form-group {
    display: flex;
    flex-direction: column;
}

.form-group.full {
    grid-column: 1 / -1;
}

label {
    margin-bottom: 7px;
    font-weight: bold;
    color: #425066;
}

input,
select,
textarea {
    width: 100%;
    padding: 13px;
    border: 1px solid #d7dee7;
    border-radius: 8px;
    font-size: 15px;
    outline: none;
}

input:focus,
select:focus,
textarea:focus {
    border-color: #117c75;
}

textarea {
    min-height: 100px;
    resize: vertical;
}

button {
    background: #117c75;
    color: white;
    border: none;
    padding: 14px 25px;
    border-radius: 8px;
    font-size: 16px;
    font-weight: bold;
    cursor: pointer;
}

button:hover {
    background: #0d655f;
}

.message {
    padding: 14px 18px;
    border-radius: 8px;
    margin-bottom: 20px;
    font-weight: bold;
}

.success {
    background: #e5f7ef;
    color: #13734c;
}

.error {
    background: #fdeaea;
    color: #b42318;
}

.animal-card {
    border: 1px solid #e0e5eb;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 15px;
}

.animal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
}

.animal-header h4 {
    margin: 0;
    font-size: 20px;
}

.species {
    background: #e7f5f3;
    color: #117c75;
    padding: 6px 12px;
    border-radius: 20px;
    font-weight: bold;
}

.details {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
}

.detail {
    background: #f7f9fb;
    padding: 12px;
    border-radius: 8px;
}

.detail strong {
    display: block;
    color: #697789;
    font-size: 13px;
    margin-bottom: 5px;
}

.detail span {
    font-weight: bold;
}

.empty {
    text-align: center;
    padding: 40px;
    color: #697789;
}

@media (max-width: 700px) {

    .form-grid {
        grid-template-columns: 1fr;
    }

    .form-group.full {
        grid-column: auto;
    }

    .details {
        grid-template-columns: 1fr;
    }

    .header {
        padding: 15px 20px;
    }

    .container {
        margin-top: 20px;
    }

}

</style>

</head>

<body>


<div class="header">

    <h1>🐾 AI Vet Tech</h1>

    <a href="dashboard.php">← Dashboard</a>

</div>


<div class="container">


    <div class="page-title">

        <h2>🐾 Animal Records</h2>

        <p>
            Manage animal information, medical history and veterinary records.
        </p>

    </div>


    <?php if ($message !== ""): ?>

        <div class="message <?php echo $message_type; ?>">

            <?php echo htmlspecialchars($message); ?>

        </div>

    <?php endif; ?>


    <!-- ADD ANIMAL -->

    <div class="card">

        <h3>➕ Add New Animal</h3>

        <form method="POST">

            <div class="form-grid">


                <div class="form-group">

                    <label>Animal Name *</label>

                    <input
                        type="text"
                        name="animal_name"
                        placeholder="Example: Max"
                        required
                    >

                </div>


                <div class="form-group">

                    <label>Species *</label>

                    <select name="species" required>

                        <option value="">Select species</option>

                        <option value="Dog">Dog</option>

                        <option value="Cat">Cat</option>

                        <option value="Bird">Bird</option>

                        <option value="Rabbit">Rabbit</option>

                        <option value="Horse">Horse</option>

                        <option value="Cattle">Cattle</option>

                        <option value="Goat">Goat</option>

                        <option value="Sheep">Sheep</option>

                        <option value="Pig">Pig</option>

                        <option value="Other">Other</option>

                    </select>

                </div>


                <div class="form-group">

                    <label>Breed</label>

                    <input
                        type="text"
                        name="breed"
                        placeholder="Example: German Shepherd"
                    >

                </div>


                <div class="form-group">

                    <label>Sex</label>

                    <select name="sex">

                        <option value="">Select sex</option>

                        <option value="Male">Male</option>

                        <option value="Female">Female</option>

                    </select>

                </div>


                <div class="form-group">

                    <label>Age</label>

                    <input
                        type="text"
                        name="age"
                        placeholder="Example: 3 years"
                    >

                </div>


                <div class="form-group">

                    <label>Owner Name</label>

                    <input
                        type="text"
                        name="owner_name"
                        placeholder="Animal owner"
                    >

                </div>


                <div class="form-group full">

                    <label>Medical History</label>

                    <textarea
                        name="medical_history"
                        placeholder="Enter previous medical information..."
                    ></textarea>

                </div>


                <div class="form-group full">

                    <label>Notes</label>

                    <textarea
                        name="notes"
                        placeholder="Additional notes..."
                    ></textarea>

                </div>


                <div class="form-group full">

                    <button type="submit">

                        🐾 Save Animal Record

                    </button>

                </div>


            </div>

        </form>

    </div>


    <!-- ANIMAL LIST -->

    <div class="card">

        <h3>📋 My Animal Records</h3>


        <?php if (count($animals) === 0): ?>

            <div class="empty">

                <div style="font-size: 45px;">🐾</div>

                <h3>No animal records yet</h3>

                <p>
                    Add your first animal using the form above.
                </p>

            </div>

        <?php else: ?>


            <?php foreach ($animals as $animal): ?>

                <div class="animal-card">


                    <div class="animal-header">

                        <h4>
                            🐾
                            <?php echo htmlspecialchars($animal['animal_name']); ?>
                        </h4>

                        <span class="species">

                            <?php echo htmlspecialchars($animal['species']); ?>

                        </span>

                    </div>


                    <div class="details">


                        <div class="detail">

                            <strong>Breed</strong>

                            <span>
                                <?php
                                echo htmlspecialchars(
                                    $animal['breed'] ?: 'Not specified'
                                );
                                ?>
                            </span>

                        </div>


                        <div class="detail">

                            <strong>Sex</strong>

                            <span>
                                <?php
                                echo htmlspecialchars(
                                    $animal['sex'] ?: 'Not specified'
                                );
                                ?>
                            </span>

                        </div>


                        <div class="detail">

                            <strong>Age</strong>

                            <span>
                                <?php
                                echo htmlspecialchars(
                                    $animal['age'] ?: 'Not specified'
                                );
                                ?>
                            </span>

                        </div>


                        <div class="detail">

                            <strong>Owner</strong>

                            <span>
                                <?php
                                echo htmlspecialchars(
                                    $animal['owner_name'] ?: 'Not specified'
                                );
                                ?>
                            </span>

                        </div>


                        <div class="detail">

                            <strong>Added</strong>

                            <span>
                                <?php
                                echo htmlspecialchars(
                                    $animal['created_at']
                                );
                                ?>
                            </span>

                        </div>


                    </div>


                    <?php if (!empty($animal['medical_history'])): ?>

                        <div style="margin-top:15px;">

                            <strong>Medical History:</strong>

                            <p>
                                <?php
                                echo nl2br(
                                    htmlspecialchars(
                                        $animal['medical_history']
                                    )
                                );
                                ?>
                            </p>

                        </div>

                    <?php endif; ?>


                    <?php if (!empty($animal['notes'])): ?>

                        <div style="margin-top:15px;">

                            <strong>Notes:</strong>

                            <p>
                                <?php
                                echo nl2br(
                                    htmlspecialchars(
                                        $animal['notes']
                                    )
                                );
                                ?>
                            </p>

                        </div>

                    <?php endif; ?>


                </div>

            <?php endforeach; ?>


        <?php endif; ?>


    </div>


</div>

</body>

</html>