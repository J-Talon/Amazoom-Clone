function editCart(action, id) {
    window.location.href=`/${action}Cart?id=${id}${window.location.search.replace('?','&')}${
        document.getElementById(`quantity-${id}`)?.value > 0 ? "&quantity=" + document.getElementById(`quantity-${id}`).value : ""
        }${document.getElementById('username')?.value ? "&username=" + document.getElementById('username').value : ""}`;
}