require "spec_helper"
require_relative "../shared/common"

feature "Inventory Pagination", type: :feature do
  scenario "change pagination settings" do
    pool = FactoryBot.create(:inventory_pool)
    user = FactoryBot.create(:user)
    FactoryBot.create(:access_right, inventory_pool: pool, user: user, role: :inventory_manager)

    400.times do |index|
      FactoryBot.create(:leihs_model, product: "Model", version: "Version #{index + 1}")
    end

    login(user)
    find("nav button", text: "Inventar").click
    click_on pool.name
    expect(page).to have_content("Inventarliste - #{pool.name}")

    visit_with_query_param("with_items", "false")
    change_page_size(20)
    validate_pagination_state(1, 20, "1-20", 20, 400)

    navigate_to_last_page
    validate_pagination_state(20, 20, "381-400", 20, 400)

    change_page_size(50)
    validate_pagination_state(8, 50, "351-400", 8, 400)

    change_page_size(100)
    validate_pagination_state(4, 100, "301-400", 4, 400)

    change_page_size(10)
    validate_pagination_state_inner_pos(4, 10, "31-40", 40, 400)

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

  def validate_pagination_state_inner_pos(page, size, range_text, last_page, total_items)
    pagination = find('[aria-label="pagination"]', match: :first)
    expect(pagination.find("ul li a.shadow-sm", match: :first)).to have_content(page.to_s)
    expect(pagination.all("ul li a").last).to have_content(last_page.to_s)
    expect(pagination.all("a").last).to_not be_disabled
    expect(pagination.all("a").first).to_not be_disabled
    expect(pagination.find(:xpath, "following-sibling::*[1]")).to have_selector("span", text: /#{range_text}.*#{total_items}/)
    expect(CGI.parse(URI.parse(current_url).query)).to include("page" => [page.to_s], "size" => [size.to_s])
  end

  def validate_pagination_state(page, size, range_text, last_page, total_items, query_params = nil)
    pagination = find('[aria-label="pagination"]', match: :first)
    expect(pagination.find("button", match: :first)).to be_disabled
    expect(pagination.find("ul li a.shadow-sm")).to have_content(page.to_s)
    expect(pagination.all("ul li a").last).to have_content(last_page.to_s)
    expect(pagination.find(:xpath, "following-sibling::*[1]")).to have_selector("span", text: /#{range_text}.*#{total_items}/)

    # Default query params to include page and size unless overridden
    query_params ||= {"page" => [page.to_s], "size" => [size.to_s]}
    expect(CGI.parse(URI.parse(current_url).query)).to include(query_params)
  end

  def navigate_to_last_page
    find('[aria-label="pagination"] ul li a', text: "20", match: :first).click
  end

  def change_page_size(size)
    dropdown = find('[aria-label="pagination"] + * + * button', match: :first)
    dropdown.click
    find('[role="menuitemradio"]', text: size.to_s, match: :first).click
  end

  def validate_ellipsis_and_navigation(current, prev, next_page)
    current_page = find('[aria-label="pagination"] ul li a.shadow-sm', match: :first)
    expect(current_page).to have_content(current.to_s)
    expect(current_page.find(:xpath, "ancestor::*[1]/preceding-sibling::*[1]")).to have_selector("a", text: prev.to_s)
    expect(current_page.find(:xpath, "ancestor::*[1]/following-sibling::*[1]")).to have_selector("a", text: next_page.to_s)
    expect(current_page.find(:xpath, "ancestor::*[1]/preceding-sibling::*[2]")).to have_selector("span > svg.lucide-ellipsis")
    expect(current_page.find(:xpath, "ancestor::*[1]/following-sibling::*[2]")).to have_selector("span > svg.lucide-ellipsis")
  end

  def navigate_to_page(page)
    find('[aria-label="pagination"] ul li a', text: page.to_s, match: :first).click
  end

  def validate_prev_page(prev)
    expect(find('[aria-label="pagination"] ul li a.shadow-sm', match: :first).find(:xpath, "ancestor::*[1]/preceding-sibling::*[1]")).to have_selector("a", text: prev.to_s)
  end

  def validate_next_page(next_page)
    expect(find('[aria-label="pagination"] ul li a.shadow-sm', match: :first).find(:xpath, "ancestor::*[1]/following-sibling::*[1]")).to have_selector("a", text: next_page.to_s)
  end
end
