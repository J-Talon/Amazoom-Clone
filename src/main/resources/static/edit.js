function edit(id, sender) {
    let query = "";
    for (const field of document.querySelectorAll("input")) {
        if (!field.readOnly) {
            query += `&key=${field.id.toLowerCase()}&value=${field.value}`;
        }
    }

    if (query === "") {
        alert("No editable fields");
    } else {
        window.location.href=`./edit?id=${id ?? sender.getAttribute('data-id')}${query}`;
    }
}