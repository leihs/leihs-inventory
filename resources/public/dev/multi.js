$(document).ready(function () {
    let selectedItems = [];

    // Function to fetch search results via AJAX
    function fetchSuggestions(query) {
        if (query.length < 2) {
            $(".suggestions").hide();
            return;
        }

        let requestData = {
            page: 1,
            size: 200,
            result_type: "Normal"
        };

        if (query.length > 0) {
            requestData.search_term = query;
        }

        $.ajax({
            url: `http://localhost:3260/inventory/8bd16d45-056d-5590-bc7f-12849f034351/items-with-model-info`,
            type: "GET",
            headers: { 'Accept': 'application/json' },
            data: requestData,
            success: function (response) {
                let filtered = response.filter(item => !selectedItems.includes(item.inventory_code));

                if (filtered.length > 0) {
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

    // Use MutationObserver to detect when the main form is added dynamically
    const observer = new MutationObserver(function (mutationsList) {
        for (let mutation of mutationsList) {
            if (mutation.type === "childList") {
                let searchBox = document.getElementById("searchBox");
                if (searchBox) {
                    console.log("🔄 Form Loaded - Rebinding Events...");

                    // Reattach input event to search box
                    $(document).off("input", "#searchBox").on("input", "#searchBox", function () {
                        let query = $(this).val().trim();
                        fetchSuggestions(query);
                    });

                    // Reattach click event for selecting suggestions
                    $(document).off("click", ".suggestion-item").on("click", ".suggestion-item", function () {
                        let selectedItemData = {
                            inventory_code: $(this).data("inventory"),
                            manufacturer: $(this).data("manufacturer"),
                            product: $(this).data("product")
                        };

                        if (!selectedItems.includes(selectedItemData.inventory_code)) {
                            selectedItems.push(selectedItemData.inventory_code);

                            $(".selected-items").append(`
                                <div class="selected-item" data-inventory="${selectedItemData.inventory_code}">
                                    ${selectedItemData.inventory_code} | ${selectedItemData.manufacturer} / ${selectedItemData.product}
                                    <span>&times;</span>
                                </div>
                            `);
                        }

                        $("#searchBox").val('');
                        $(".suggestions").hide();
                    });

                    // Reattach click event for removing selected items
                    $(document).off("click", ".selected-item span").on("click", ".selected-item span", function () {
                        let itemToRemove = $(this).parent().data("inventory");
                        selectedItems = selectedItems.filter(item => item !== itemToRemove);
                        $(this).parent().remove();
                    });

                    // Hide suggestions when clicking outside, but NOT when clicking inside selected-items
                    $(document).off("click", ".outside-click-handler").on("click", ".outside-click-handler", function (e) {
                        if (!$(e.target).closest(".multi-select-container").length &&
                            !$(e.target).closest(".selected-items").length) {
                            $(".suggestions").hide();
                        }
                    });
                }
            }
        }
    });

    // Start observing the entire document (or a specific parent container)
    observer.observe(document.body, { childList: true, subtree: true });

    // Attach a document-wide event listener to handle outside clicks without removing other click handlers
    $(document).on("click", function (e) {
        if (!$(e.target).closest(".multi-select-container").length &&
            !$(e.target).closest(".selected-items").length &&
            !$(e.target).hasClass("suggestion-item")) {
            $(".suggestions").hide();
        }
    });
});
