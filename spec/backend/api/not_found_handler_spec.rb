require "spec_helper"
require_relative "_shared"

describe "custom-not-found-handler content negotiation" do
  before :each do
    @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:admin)
  end

  context "inventory routes with HTML Accept header" do
    it "returns 200 SPA for text/html" do
      client = Faraday.new(url: api_base_url, headers: {:accept => "text/html", "Cookie" => @user_cookies_str})
      response = client.get("/inventory/nonexistent-route")

      expect(response.status).to eq(200)
      expect(response.headers["content-type"]).to include("text/html")
      expect(response.body).to include("<title>Inventory</title>")
    end

    it "returns 200 SPA for */*" do
      client = Faraday.new(url: api_base_url, headers: {:accept => "*/*", "Cookie" => @user_cookies_str})
      response = client.get("/inventory/another-nonexistent")

      expect(response.status).to eq(200)
      expect(response.headers["content-type"]).to include("text/html")
      expect(response.body).to include("<title>Inventory</title>")
    end

    it "returns 200 SPA for empty Accept header (defaults to */*)" do
      client = Faraday.new(url: api_base_url, headers: {"Cookie" => @user_cookies_str})
      response = client.get("/inventory/no-accept-header")

      expect(response.status).to eq(200)
      expect(response.headers["content-type"]).to include("text/html")
      expect(response.body).to include("<title>Inventory</title>")
    end
  end

  context "inventory routes with JSON Accept header" do
    it "returns 404 JSON for application/json" do
      client = session_auth_plain_faraday_json_client(cookies: @user_cookies)
      response = client.get("/inventory/api-nonexistent")

      expect(response.status).to eq(404)
      expect(response.headers["content-type"]).to include("application/json")
      expect(response.body).to have_key("error")
      expect(response.body["error"]).to eq("Not Found")
    end
  end

  context "inventory routes with image Accept header" do
    it "returns 404 text/plain for image/png" do
      client = Faraday.new(url: api_base_url, headers: {:accept => "image/png", "Cookie" => @user_cookies_str})
      response = client.get("/inventory/nonexistent-image")

      expect(response.status).to eq(404)
      expect(response.headers["content-type"]).to eq("text/plain; charset=utf-8")
      expect(response.body).to eq("Not Found")
    end

    it "returns 404 text/plain for image/jpeg" do
      client = Faraday.new(url: api_base_url, headers: {:accept => "image/jpeg", "Cookie" => @user_cookies_str})
      response = client.get("/inventory/another-nonexistent-image")

      expect(response.status).to eq(404)
      expect(response.headers["content-type"]).to eq("text/plain; charset=utf-8")
      expect(response.body).to eq("Not Found")
    end
  end

  context "inventory routes with export Accept headers" do
    it "returns 404 JSON for text/csv" do
      client = Faraday.new(url: api_base_url, headers: {:accept => ACCEPT_CSV, "Cookie" => @user_cookies_str}) do |conn|
        conn.response :json, content_type: /\bjson$/
        conn.adapter Faraday.default_adapter
      end
      response = client.get("/inventory/nonexistent-export")

      expect(response.status).to eq(404)
      expect(response.headers["content-type"]).to include("application/json")
      expect(response.body).to have_key("error")
    end

    it "returns 404 JSON for Excel format" do
      client = Faraday.new(url: api_base_url, headers: {:accept => ACCEPT_XLSX, "Cookie" => @user_cookies_str}) do |conn|
        conn.response :json, content_type: /\bjson$/
        conn.adapter Faraday.default_adapter
      end
      response = client.get("/inventory/nonexistent-excel")

      expect(response.status).to eq(404)
      expect(response.headers["content-type"]).to include("application/json")
      expect(response.body).to have_key("error")
    end
  end

  context "inventory routes with unsupported Accept header" do
    it "returns 406 for application/xml" do
      client = Faraday.new(url: api_base_url, headers: {:accept => "application/xml", "Cookie" => @user_cookies_str})
      response = client.get("/inventory/unsupported-format")

      expect(response.status).to eq(406)
      expect(response.headers["content-type"]).to eq("text/plain; charset=utf-8")
      expect(response.body).to eq("Not Acceptable")
    end
  end

  context "non-inventory routes" do
    it "returns 404 JSON for HTML Accept on non-inventory route" do
      client = Faraday.new(url: api_base_url, headers: {:accept => "text/html", "Cookie" => @user_cookies_str}) do |conn|
        conn.response :json, content_type: /\bjson$/
        conn.adapter Faraday.default_adapter
      end
      response = client.get("/other/nonexistent-path")

      expect(response.status).to eq(404)
      expect(response.headers["content-type"]).to include("application/json")
      expect(response.body).to have_key("error")
    end
  end
end
