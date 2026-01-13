require 'spec_helper'
require_relative '_shared'

describe 'Content Negotiation - PR #79 Issues' do
  let(:unauthenticated_client) { plain_faraday_json_client }
  let(:pool_id) { '8bd16d45-056d-5590-bc7f-12849f034351' }
  let(:non_existent_model_id) { '2aa391d8-447c-4b28-bc9a-136ebe2db3ef' }

  # Test scenarios from PR #79 curl commands - documenting current behavior
  describe 'Current Behavior Documentation' do
    describe 'Scenario 1: GET /models/ with Accept: application/json (unauthenticated)' do
      it 'returns 401 with proper error message' do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/", 'application/json')
        expect(resp.status).to eq(401)
        expect(resp.body['status']).to eq('failure')
        expect(resp.body['message']).to eq('Not authenticated')
      end
    end

    describe 'Scenario 2: GET /models/ with Accept: */* (unauthenticated)' do
      it 'returns SPA (HTML)' do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/", '*/*')
        expect(resp.status).to eq(200)
        expect(resp.headers['Content-Type']).to match(%r{text/html})
      end
    end

    describe 'Scenario 3: GET /models/ with Accept: image/* (unauthenticated)' do
      it 'currently returns 500 coercion error (BUG from PR #79)' do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/", 'image/*')
        expect(resp.status).to eq(500)
        expect(resp.body['reason']).to eq('Coercion-Error')
        expect(resp.body['detail']).to eq('Response coercion failed')
        expect(resp.body['coercion-type']).to eq('schema')
        expect(resp.body['scope']).to eq('response/body')
      end
    end

    describe 'Scenario 4: GET /models/:id/ with Accept: image/* (non-existent, unauthenticated)' do
      it 'returns 401 (correct behavior)' do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/#{non_existent_model_id}/",
                               'image/*')
        expect(resp.status).to eq(401)
        expect(resp.body['status']).to eq('failure')
        expect(resp.body['message']).to eq('Not authenticated')
      end
    end

    describe 'Scenario 5: GET /models/:id/ with Accept: */* (non-existent, unauthenticated)' do
      it 'returns SPA (HTML)' do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/#{non_existent_model_id}/", '*/*')
        expect(resp.status).to eq(200)
        expect(resp.headers['Content-Type']).to match(%r{text/html})
      end
    end

    describe 'Scenario 6: GET /models/:id/ with Accept: application/json (non-existent, unauthenticated)' do
      it 'returns 401 (auth check happens first)' do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/#{non_existent_model_id}/",
                               'application/json')
        expect(resp.status).to eq(401)
        expect(resp.body['status']).to eq('failure')
        expect(resp.body['message']).to eq('Not authenticated')
      end
    end
  end

  # Expected behavior tests (will fail until bugs are fixed)
  describe 'Expected Behavior (after fixing PR #79 issues)' do
    describe 'Scenario 3 Fixed: GET /models/ with Accept: image/* (unauthenticated)' do
      it 'should return 401 instead of coercion error' do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/", 'image/*')
        expect(resp.status).to eq(401)
        expect(resp.body['status']).to eq('failure')
        expect(resp.body['message']).to eq('Not authenticated')
      end
    end

    describe 'Additional test: Accept: image/* on non-image resources should return 406' do
      it 'authenticated user should get 406 for image/* on models endpoint' do
        # This would need authentication setup when login issues are resolved
        skip 'Requires working authentication to test'
        # resp = get_with_accept(authenticated_client, "/inventory/#{pool_id}/models/", 'image/*')
        # expect(resp.status).to eq(406)
        # expect(resp.body).to eq('Not Acceptable')
      end
    end
  end

  private

  def get_with_accept(client, url, accept_header)
    client.get url do |req|
      req.headers['Accept'] = accept_header
      req.headers['x-csrf-token'] = X_CSRF_TOKEN
    end
  end
end
