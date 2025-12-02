require "spec_helper"
require_relative "../shared/common"

feature "Inventory Pagination ", type: :feature do
  let!(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:user) { FactoryBot.create(:user, language_locale: "en-GB") }

  before(:each) do
    FactoryBot.create(:access_right, inventory_pool: pool, user: user, role: :inventory_manager)

    400.times do |index|
      FactoryBot.create(:leihs_model, product: "Model", version: "Version #{index + 1}")
    end

    login(user)
    visit "/inventory"
    expect(page).to have_content("Inventory", wait: 30)
    find("nav button", text: "Inventory").click
    within('[role="menu"]', match: :first) do
      click_on pool.name
    end
    expect(page).to have_content pool.name
  end

  scenario "works" do
    visit_with_query_param("with_items", "false")
    change_page_size(20)
    validate_pagination_state(1, 20, "1-20", 20, 400)

    navigate_to_last_page
    validate_pagination_state(20, 20, "381-400", 20, 400)

    change_page_size(50)
    validate_pagination_state(1, 50, "1-50", 8, 400)

    change_page_size(100)
    validate_pagination_state(1, 100, "1-100", 4, 400)

    change_page_size(10)
    validate_pagination_state(1, 10, "1-10", 40, 400)

    visit_with_query_param("page", "4")
    validate_ellipsis_and_navigation(4, 3, 5)
    navigate_to_page(40)
    validate_prev_page(39)
    navigate_to_page(1)
    validate_next_page(2)

    change_page_size(50)
    validate_pagination_state(1, 50, "1-50", 8, 400)

    remove_query_param("size")
    validate_pagination_state(1, 10, "1-10", 40, 400, {"page" => ["1"]})
  end

  scenario "shortcuts work" do
    visit_with_query_param("with_items", "false")

    expect(page).to have_content("Model Version 1", wait: 20)
    expect(page).to have_current_path(/page=1/)

    # Trigger Shift + Alt + ArrowRight
    page.driver.browser.action.key_down(:shift)
      .key_down(:alt)
      .send_keys(:arrow_right)
      .key_up(:alt)
      .key_up(:shift)
      .perform

    expect(page).to have_content("Model Version 144", wait: 20)
    # Wait for the page to update and check if the query param is page=2
    expect(page).to have_current_path(/page=2/)

    # Trigger Shift + Alt + ArrowLeft
    page.driver.browser.action.key_down(:shift)
      .key_down(:alt)
      .send_keys(:arrow_left)
      .key_up(:alt)
      .key_up(:shift)
      .perform

    expect(page).to have_content("Model Version 1", wait: 20)
    # Wait for the page to update and check if the query param is back to page=1
    expect(page).to have_current_path(/page=1/)
  end

  def remove_query_param(key)
    uri = URI.parse(current_url)
    query_params = CGI.parse(uri.query)
    query_params.delete(key)
    uri.query = URI.encode_www_form(query_params)
    visit uri.to_s
  end

  def visit_with_query_param(key, value)
    uri = URI.parse(current_url)
    query_params = CGI.parse(uri.query).merge(key => [value])
    uri.query = URI.encode_www_form(query_params)
    visit uri.to_s
  end

  def validate_pagination_state(page, size, range_text, last_page, total_items, query_params = nil)
    pagination = find('[data-test-id="pagination-container"]')
    if page == last_page
      expect(pagination).to have_link("pagination-previous")
      expect(pagination).to have_button("pagination-next", disabled: true)
    elsif page < last_page && page > 1
      expect(pagination).to have_link("pagination-previous")
      expect(pagination).to have_link("pagination-next")
      expect(pagination).to have_link("pagination-last-page", text: last_page.to_s)
    else
      expect(pagination).to have_button("pagination-previous", disabled: true)
      expect(pagination).to have_link("pagination-next")
      expect(pagination).to have_link("pagination-last-page", text: last_page.to_s)
    end

    expect(pagination).to have_link("pagination-current-page", text: page.to_s)
    expect(pagination).to have_content(/#{range_text}.*#{total_items}/)

    # Default query params to include page and size unless overridden
    query_params ||= {"page" => [page.to_s], "size" => [size.to_s]}
    expect(CGI.parse(URI.parse(current_url).query)).to include(query_params)
  end

  def navigate_to_last_page
    click_on "pagination-last-page"
  end

  def change_page_size(size)
    click_on "pagination-size-button"
    expect(page).to have_css('[data-test-id="pagination-size-dropdown"]', visible: true, wait: 10)
    within '[data-test-id="pagination-size-dropdown"]' do
      click_on size.to_s
    end
  end

  def validate_ellipsis_and_navigation(current, prev_page, next_page)
    current_page = find('[data-test-id="pagination-current-page"]')
    pagination = find('[data-test-id="pagination"]')
    expect(current_page).to have_content(current.to_s)
    expect(current_page.find(:xpath, "ancestor::*[1]/preceding-sibling::*[2]")).to have_selector("span > svg.lucide-ellipsis")
    expect(current_page.find(:xpath, "ancestor::*[1]/following-sibling::*[2]")).to have_selector("span > svg.lucide-ellipsis")

    expect(pagination).to have_link("pagination-previous-page", text: prev_page.to_s)
    expect(pagination).to have_link("pagination-next-page", text: next_page.to_s)

    expect(pagination).to have_link("pagination-next")
    expect(pagination).to have_link("pagination-previous")
  end

  def navigate_to_page(page)
    pagination = find('[data-test-id="pagination"]')
    within pagination do
      expect(pagination).to have_link(page.to_s)
      click_on page.to_s
    end
  end

  def validate_prev_page(prev_page)
    pagination = find('[data-test-id="pagination"]')
    expect(pagination).to have_link("pagination-previous-page", text: prev_page.to_s)
    # expect(find('[aria-label="pagination"] ul li a.shadow-sm', match: :first).find(:xpath, "ancestor::*[1]/preceding-sibling::*[1]")).to have_selector("a", text: prev.to_s)
  end

  def validate_next_page(next_page)
    pagination = find('[data-test-id="pagination"]')
    expect(pagination).to have_link("pagination-next-page", text: next_page.to_s)
    # expect(find('[aria-label="pagination"] ul li a.shadow-sm', match: :first).find(:xpath, "ancestor::*[1]/following-sibling::*[1]")).to have_selector("a", text: next_page.to_s)
  end
end
