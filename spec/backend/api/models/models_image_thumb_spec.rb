require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Swagger Inventory Endpoints - Models" do
  context "when fetching models for an inventory pool" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      @model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Model", is_package: false, version: "1")
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }

    context "POST and GET /inventory/:pool_id/models when creating new models" do
      let(:url) { "/inventory/#{@inventory_pool.id}/list/" }

      context "when a model has an image but no thumbnail" do
        before :each do
          @image = FactoryBot.create(:image, :for_leihs_model, target: @model, real_filename: "anon.jpg")
        end

        it "returns url of default-image when the model has images" do
          resp = client.get url

          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(@model.id)
          expect(resp.body.count).to eq(1)
        end
      end

      context "when a model has no images or thumbnails" do
        it "returns image_url as nil when the model has no images at all" do
          resp = client.get url

          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(@model.id)
          expect(resp.body[0]["url"]).to be_nil
          expect(resp.body.count).to eq(1)
        end
      end

      context "when a model has multiple thumbnails and images" do
        before :each do
          # image with two thumbnails
          @thumbnail = FactoryBot.create(:image, :for_leihs_model,
            thumbnail: true,
            target: @model,
            filename: "anon_thumb.jpg",
            real_filename: "anon.jpg")
          @thumbnail2 = FactoryBot.create(:image, :for_leihs_model,
            thumbnail: true,
            target: @model,
            filename: "sap_thumb.jpg",
            real_filename: "sap.png")
          @image = FactoryBot.create(:image, :for_leihs_model,
            target: @model,
            thumbnails: [@thumbnail, @thumbnail2],
            real_filename: "anon.jpg")

          # image with one thumbnail
          @thumbnail3 = FactoryBot.create(:image, :for_leihs_model,
            thumbnail: true,
            target: @model,
            filename: "sap2_thumb.jpg",
            real_filename: "sap.png")
          @image2 = FactoryBot.create(:image, :for_leihs_model,
            target: @model,
            thumbnails: [@thumbnail3],
            filename: "anon2.jpg",
            real_filename: "anon.jpg")
        end

        it "returns image_url for the first thumbnail if cover_image_id is not set" do
          resp = client.get url

          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(@model.id)
          expect(resp.body[0]["url"]).to end_with(@image.id)
          expect(resp.body.count).to eq(1)
        end

        it "returns image_url for the image specified by cover_image_id (@image)" do
          @model.update(cover_image_id: @image.id)

          resp = client.get url

          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(@model.id)
          expect(resp.body[0]["url"]).to end_with(@image.id)
          expect(resp.body.count).to eq(1)
        end

        it "returns image_url for the image specified by cover_image_id (@image2)" do
          @model.update(cover_image_id: @image2.id)

          resp = client.get url

          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(@model.id)
          expect(resp.body[0]["url"]).to end_with(@image2.id)
          expect(resp.body.count).to eq(1)
        end
      end

      context "when a model has only a regular image and no thumbnail" do
        before :each do
          @image = FactoryBot.create(:image, :for_leihs_model,
            target: @model,
            real_filename: "anon.jpg")
        end

        it "omits the image_url key entirely if the model has images but no thumbnails and no cover_image_id" do
          resp = client.get url

          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(@model.id)
          expect(resp.body[0]["url"]).to end_with(@image.id)
          expect(resp.body.count).to eq(1)
        end
      end
    end
  end
end
