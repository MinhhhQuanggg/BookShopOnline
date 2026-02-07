$(document).ready(function () {

    function updateCart(bookId, quantity) {
        if (quantity < 1) quantity = 1;

        $.ajax({
            url: '/cart/update',
            type: 'POST',
            data: {
                bookId: bookId,
                quantity: quantity
            },
            success: function () {
                location.reload(); // reload để cập nhật total
            },
            error: function () {
                alert('Cập nhật số lượng thất bại');
            }
        });
    }

    // Nhập tay số lượng
    $('.quantity').on('change', function () {
        let quantity = parseInt($(this).val());
        let bookId = $(this).data('id');
        updateCart(bookId, quantity);
    });

    // Nút +
    $('.btn-plus').on('click', function () {
        let bookId = $(this).data('id');
        let input = $(this).siblings('.quantity');
        let quantity = parseInt(input.val()) + 1;
        input.val(quantity);
        updateCart(bookId, quantity);
    });

    // Nút -
    $('.btn-minus').on('click', function () {
        let bookId = $(this).data('id');
        let input = $(this).siblings('.quantity');
        let quantity = parseInt(input.val()) - 1;

        if (quantity < 1) quantity = 1;
        input.val(quantity);
        updateCart(bookId, quantity);
    });

});
