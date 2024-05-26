function search(admin) {
    if (admin) {
        let term = document.getElementById("term").value;
        let by = document.getElementById("by").value;
        let sortBy = document.getElementById("sortBy").value;
        let sortDirection = document.getElementById("direction").value;
        window.location.href = "/results?term=" + term + "&by=" + by + "&sortBy=" + sortBy + "&direction=" + sortDirection;
    } else {
        let term = document.getElementById("term").value;
        let sortBy = document.getElementById("sortBy").value;
        let sortDirection = document.getElementById("direction").value;
        window.location.href = "/results?term=" + term + "&sortBy=" + sortBy + "&direction=" + sortDirection;
    }
}