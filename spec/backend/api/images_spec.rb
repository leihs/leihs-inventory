require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

describe "Inventory API Endpoints - Image Handling" do
  context "when fetching images for a specific inventory pool" do
    include_context :setup_access_rights

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
      @pool = create(:inventory_pool)
      create(:access_right, user: @user, inventory_pool: @pool, role: "inventory_manager")
    end

    let(:any_uuid) { Faker::Internet.uuid }
    let(:different_content_type) { "image/gif" }
    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }

    let(:url) { "/inventory/#{@pool.id}/models/#{any_uuid}/images/" }
    let(:resp) { client.get url }
    let(:single_image_id) { resp.body[0]["id"] }
    let(:image_id) { resp.body[1]["id"] }
    let(:image_content_type) { resp.body[0]["content_type"] }

    context "GET /inventory/images" do
      it "retrieves all images and returns status 200" do
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(3)
      end

      it "retrieves paginated image results and returns status 200" do
        resp = client.get "#{url}?page=2&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
        expect(resp.body["pagination"]["total_rows"]).to eq(3)
      end
    end

    context "Fetch image data as JSON" do
      it "retrieves image details by ID and returns status 200" do
        resp = client.get "#{url}#{image_id}"
        expect(resp.status).to eq(200)
      end

      it "retrieves image thumbnail by ID and returns status 200" do
        resp = client.get "#{url}#{image_id}/thumbnail"
        expect(resp.status).to eq(200)
      end

      context "Blocks fetch image with text/html  (FF/new tab)" do
        it "retrieves image thumbnail by ID and returns status 200" do
          resp = client.get "#{url}#{image_id}/thumbnail" do |req|
            req.headers["Accept"] = "text/html"
          end
          expect(resp.status).to eq(200)
        end

        it "retrieves image by ID and returns status 200" do
          resp = client.get "#{url}#{image_id}" do |req|
            req.headers["Accept"] = "text/html"
          end
          expect(resp.status).to eq(200)
        end
      end

      context "Fetch image with correct content_type" do
        it "retrieves image thumbnail by ID and returns status 200" do
          resp = client.get "#{url}#{image_id}/thumbnail" do |req|
            req.headers["Accept"] = image_content_type
          end
          expect(resp.status).to eq(200)
        end

        it "retrieves image by ID and returns status 200" do
          resp = client.get "#{url}#{image_id}" do |req|
            req.headers["Accept"] = image_content_type
          end
          expect(resp.status).to eq(200)
        end
      end

      context "Fetch image with different_content_type" do
        it "retrieves image thumbnail and returns status 406" do
          resp = client.get "#{url}#{image_id}/thumbnail" do |req|
            req.headers["Accept"] = different_content_type
          end
          expect(resp.status).to eq(406)
        end

        it "retrieves image and returns status 406" do
          resp = client.get "#{url}#{image_id}" do |req|
            req.headers["Accept"] = different_content_type
          end
          expect(resp.status).to eq(406)
        end
      end
    end

    context "Fetch image data as an image" do
      it "returns error when fetching image by ID as a raw image format" do
        resp = client.get "#{url}#{single_image_id}"
        expect(resp.status).to eq(200)
      end

      it "returns error when fetching image thumbnail as a raw image format" do
        resp = client.get "#{url}#{single_image_id}/thumbnail"
        expect(resp.status).to eq(404)
      end
    end
  end
end
