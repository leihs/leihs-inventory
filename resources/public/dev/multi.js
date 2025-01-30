$(document).ready(function () {
    let selectedItems = [];

    // Function to fetch search results (mockup AJAX request)
    // function fetchSuggestions(query) {
    //     if (query.length < 2) {
    //         $(".suggestions").hide();
    //         return;
    //     }
    //
    //     // Mock data (Replace this with an actual AJAX call)
    //     let allItems = ["Apple", "Banana", "Cherry", "Mango", "Grapes", "Pineapple", "Strawberry", "Blueberry"];
    //
    //     let filtered = allItems.filter(item => item.toLowerCase().includes(query.toLowerCase()) && !selectedItems.includes(item));
    //
    //     if (filtered.length > 0) {
    //         let suggestionsHtml = filtered.map(item => `<div class="suggestion-item">${item}</div>`).join('');
    //         $(".suggestions").html(suggestionsHtml).show();
    //     } else {
    //         $(".suggestions").hide();
    //     }
    // }

    // Function to fetch search results via AJAX
    function fetchSuggestions(query) {
        // if (query.length < 2) {
        //     $(".suggestions").hide();
        //     return;
        // }

        // Prepare request parameters
        let requestData = {
            page: 1,
            size: 200,
            result_type: "Normal"
        };

        // Only add search_term if query is not empty
        if (query.length > 0) {
            requestData.search_term = query;
        }

        $.ajax({
            url: `http://localhost:3260/inventory/8bd16d45-056d-5590-bc7f-12849f034351/items-with-model-info`,
            type: "GET",
            headers: { 'accept': 'application/json' },

            data: requestData,
            success: function (response) {
                // Assuming response contains an array of item names
                debugger
                // let allItems = response.map(item => item.product);
                let allItems = response

                // Filter out already selected items
                let filtered = allItems.filter(item => !selectedItems.includes(item));

                if (filtered.length > 0) {
                    // let suggestionsHtml = filtered.map(item => `<div class="suggestion-item">${item.inventory_code} | ${item.manufacturer} / ${item.product}</div>`).join('');

                    let suggestionsHtml = filtered.map(item =>
                        `<div class="suggestion-item" 
                             data-inventory="${item.inventory_code}" 
                             data-manufacturer="${item.manufacturer}" 
                             data-product="${item.product}">
                             ${item.inventory_code} | ${item.manufacturer} / ${item.product}
                         </div>`
                    ).join('');

                    $(".suggestions").html(suggestionsHtml).show();
                } else {
                    $(".suggestions").hide();
                }
            },
            error: function () {
                console.error("Error fetching suggestions.");
            }
        });
    }

    // Handle text input
    $("#searchBox").on("input", function () {
        let query = $(this).val().trim();
        fetchSuggestions(query);
    });

    // Handle item selection
    $(document).on("click", ".suggestion-item", function () {
        let selectedItem = $(this).text();
        selectedItems.push(selectedItem);
        $(".selected-items").append(`<div class="selected-item">${selectedItem} <span>&times;</span></div>`);
        $("#searchBox").val('');
        $(".suggestions").hide();
    });

    // Remove selected item
    $(document).on("click", ".selected-item span", function () {
        let itemToRemove = $(this).parent().text().slice(0, -2);
        selectedItems = selectedItems.filter(item => item !== itemToRemove);
        $(this).parent().remove();
    });

    // Hide suggestions when clicking outside
    $(document).click(function (e) {
        if (!$(e.target).closest(".multi-select-container").length) {
            $(".suggestions").hide();
        }
    });
});
