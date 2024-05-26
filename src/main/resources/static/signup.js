function signup() {
    // Check that passwords match
    if (document.getElementById("password").value !== document.getElementById("confirmPassword").value) {
        alert("Passwords do not match");
        return;
    }

    // Redirect to signup endpoint
    window.location.href = "/doSignup" +
        "?username=" + document.getElementById("username").value +
        "&password=" + document.getElementById("password").value +
        "&name=" + document.getElementById("name").value +
        "&address=" + document.getElementById("address").value;
}