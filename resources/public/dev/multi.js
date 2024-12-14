$(document).ready(function () {
    let selectedItems = [];

    window.deletedItems=[];


    window.clearMulti = function(){
        window.deletedItems = [];
        $(".selected-items").empty();
    }
    function removeFromDeletedItems(selectedItemData) {
        if (!window.deletedItems) {
            window.deletedItems = [];
        }

        window.deletedItems = window.deletedItems.filter(item =>
            item.inventory_code !== selectedItemData.inventory_code || item.id !== selectedItemData.id
        );
    }
    window.getSelectedItems = function () {

        // function getSelectedItems() {
        let selectedItems = [];

        // Select all .selected-item elements inside div[name="multi-select"]
        document.querySelectorAll('div[name="multi-select"] .selected-item').forEach(item => {
            // // // debugger


            console.log(item.getAttribute("data-inventory_code"));
            console.log(item.getAttribute("data-id"));

            selectedItems.push({
                inventory_code: item.getAttribute("data-inventory_code"),
                id: item.getAttribute("data-id"),
                // id: item.getAttribute("data-id"),
                // text: item.textContent.trim().replace('×', '') // Removes the close (×) symbol
            });
        });

        return selectedItems;
    }

// Example usage:
    console.log(getSelectedItems()); // Logs the extracted data


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

                // XXX: copy1
                let filtered = response.filter(item => !selectedItems.includes(item.inventory_code));

                if (filtered.length > 0) {
                    let suggestionsHtml = filtered.map(item =>
                    {
                        const manu = item.manufacturer ? `${item.manufacturer} /` : '';
                        return `<div class="suggestion-item" 
                             data-inventory_code="${item.inventory_code}" 
                             data-id="${item.id}" 
                             data-manufacturer="${item.manufacturer}" 
                             data-product="${item.product}">
                             ${item.inventory_code} | ${manu} ${item.product}
                         </div>`
                    }
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
                        // let selectedItemData = {
                        //     inventory_code: $(this).data("inventory"),
                        //     manufacturer: $(this).data("manufacturer"),
                        //     product: $(this).data("product")
                        // };

                        let selectedItemData = { ...$(this)[0].dataset };

                        // let selectedItemData = $(this)[0].dataset
                        // if selectedItemData.manufacturer is undefined null or "null" then set null
                        if (selectedItemData.manufacturer === "null" || selectedItemData.manufacturer === null) {
                            selectedItemData.manufacturer = null
                        }

                        if (!selectedItems.includes(selectedItemData.inventory_code)) {
                            selectedItems.push(selectedItemData.inventory_code);

                            const manu = selectedItemData.manufacturer ? `${selectedItemData.manufacturer} /` : '';

                        // debugger
                            $(".selected-items").append(`
                                <div class="selected-item" 
                                data-inventory_code="${selectedItemData.inventory_code}"
                                data-id="${selectedItemData.id}"
                                
                                >${selectedItemData.inventory_code} | ${manu} ${selectedItemData.product}
                                    <span>&times;</span>
                                </div>
                            `);

                            removeFromDeletedItems(selectedItemData);
                        }

                        $("#searchBox").val('');
                        $(".suggestions").hide();
                    });

                    // Reattach click event for removing selected items
                    $(document).off("click", ".selected-item span").on("click", ".selected-item span", function () {
                        // let itemToRemove = $(this).parent().data("inventory");

                        // let itemToRemove = { ...$(this)[0].dataset };
                        let itemToRemove = { ...$(this).parent()[0].dataset };


                        // selectedItems = selectedItems.filter(item => item !== itemToRemove);
                        selectedItems = selectedItems.filter(item => item !== itemToRemove.inventory_code);

// debugger
                        deletedItems.push(itemToRemove)

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
