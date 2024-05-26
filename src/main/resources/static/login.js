function login() {
    let username = document.getElementById("username").value;
    let password = document.getElementById("password").value;

    window.location.href = "/authenticate?username=" + username + "&password=" + password;
}