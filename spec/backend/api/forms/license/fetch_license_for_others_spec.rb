# require "spec_helper"
# require "pry"
# require_relative "../../_shared"
# require_relative "../_common"
# require "faker"

# describe "Inventory License" do
#   ["group_manager", "customer"].each do |role|
#     context "when interacting with inventory license with role=#{role}" do
#       include_context :setup_models_api_license, role
#       include_context :setup_unknown_building_room_supplier
#       include_context :generate_session_header

#       let(:cookie_header) { @cookie_header }
#       let(:client) { plain_faraday_json_client(cookie_header) }
#       let(:pool_id) { @inventory_pool.id }

#       let(:software_model) { @software_model }
#       let(:license_item) { @license_item }
#       let(:model_id) { @software_model.id }

#       let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
#       let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

#       before do
#         [path_test_pdf, path_test_txt].each do |path|
#           raise "File not found: #{path}" unless File.exist?(path)
#         end
#       end

#       context "create model" do
#         it "fetch default" do
#           resp = client.get "/inventory/#{pool_id}/license"
#           expect(resp.status).to eq(401)
#         end

#         it "fetch default" do
#           resp = client.get "/inventory/#{pool_id}/entitlement-groups"

#           expect(resp.status).to eq(200)
#           expect(resp.body.count).to eq(2)
#         end

#         it "fetch default" do
#           resp = client.get "/inventory/owners"

#           expect(resp.status).to eq(200)
#           expect(resp.body.count).to eq(2)
#         end

#         it "fetch default" do
#           resp = client.get "/inventory/supplier?search-term=a"

#           expect(resp.status).to eq(200)
#         end

#         it "fetch default" do
#           resp = client.get "inventory/manufacturers?type=Software&in-detail=true&search-term=b"

#           expect(resp.status).to eq(200)
#           expect(resp.body.count).to eq(1)
#         end

#         it "fetch default" do
#           resp = client.get "inventory/manufacturers?type=Software&in-detail=true"

#           expect(resp.status).to eq(200)
#           expect(resp.body.count).to eq(1)
#         end

#         it "creates and update license with attachment" do
#           # fetch supplier
#           resp = client.get "inventory/manufacturers?type=Software&in-detail=true"

#           expect(resp.status).to eq(200)
#           expect(resp.body.count).to eq(1)

#           supplier_id = resp.body[0]["id"]

#           # create license
#           form_data = {
#             "serial_number" => "your-serial-number",
#             "note" => "your-note",
#             "attachments" => [File.open(path_test_pdf, "rb"), File.open(path_test_txt, "rb")],
#             "invoice_date" => "2024-12-19",
#             "price" => "100",
#             "retired" => true.to_s,
#             "retired_reason" => "your-reason-retired",
#             "is_borrowable" => false.to_s,
#             "inventory_code" => "AUS45863",
#             "item_version" => "your-version",
#             "supplier_id" => supplier_id,
#             "owner_id" => pool_id,
#             "properties" => {
#               "activation_type" => "dongle",
#               "dongle_id" => "your-dongle-id",
#               "license_type" => "single_workplace",
#               "total_quantity" => "33",
#               "operating_system" => ["windows", "mac_os_x", "linux", "ios"],
#               "installation" => ["citrix", "local", "web"],
#               "license_expiration" => "2024-12-05",
#               "p4u" => "your-p4u",
#               "reference" => "investment",
#               "project_number" => "your-project-number",
#               "procured_by" => "your-procured-person",
#               "maintenance_contract" => "true",
#               "maintenance_expiration" => "2024-12-20",
#               "maintenance_currency" => "CHF",
#               "maintenance_price" => "20",
#               "quantity_allocations" => [
#                 {"quantity" => "your-key", "room" => "your-value"}
#               ]
#             }.to_json
#           }

#           resp = http_multipart_client(
#             "/inventory/#{pool_id}/models/#{model_id}/licenses",
#             form_data,
#             headers: cookie_header
#           )
#           expect(resp.status).to eq(401)
#           item_id = license_item.id

#           # fetch license
#           resp = client.get "/inventory/#{pool_id}/models/#{model_id}/licenses/#{item_id}"
#           expect(resp.status).to eq(401)

#           # update license
#           form_data = {
#             "serial_number" => "your-serial-number",
#             "note" => "your-note",
#             "invoice_date" => "2024-12-19",
#             "price" => "100",
#             "retired" => false.to_s,
#             "is_borrowable" => false.to_s,
#             "inventory_code" => "AUS45863",
#             "item_version" => "your-version",
#             "supplier_id" => nil.to_s,
#             "owner_id" => pool_id,
#             "attachments_to_delete" => [],
#             "properties" => {
#               "activation_type" => "none",
#               "license_type" => "single_workplace",
#               "total_quantity" => "33",
#               "operating_system" => [],
#               "installation" => [],
#               "license_expiration" => "2024-12-05",
#               "p4u" => "your-p4u",
#               "reference" => "investment",
#               "project_number" => "your-project-number",
#               "procured_by" => "your-procured-person",
#               "maintenance_contract" => true.to_s,
#               "maintenance_expiration" => "2024-12-20",
#               "maintenance_currency" => "CHF",
#               "maintenance_price" => "20",
#               "quantity_allocations" => []
#             }.to_json
#           }

#           resp = http_multipart_client(
#             "/inventory/#{pool_id}/models/#{model_id}/licenses/#{item_id}",
#             form_data,
#             method: :put,
#             headers: cookie_header
#           )
#           expect(resp.status).to eq(401)

#           # fetch license
#           resp = client.get "/inventory/#{pool_id}/models/#{model_id}/licenses/#{item_id}"
#           expect(resp.status).to eq(401)
#         end
#       end
#     end
#   end
# end
