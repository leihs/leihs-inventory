require 'spec_helper'
require 'pry'








shared_context :setup_models_api do
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)

    FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

    @models = 3.times.map do
      FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    end

    LeihsModel.all.each do |model|
      FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
    end

    first_model = @models.first

    # accessory
    cde=database[:accessories_inventory_pools].insert(
      accessory_id: FactoryBot.create(:accessory).id,
      inventory_pool_id: @inventory_pool.id
    )

    # FIXME
    abc=FactoryBot.create(:accessory, leihs_model: first_model)
    puts ">>> Accessory.abc: #{abc.id}"

    # binding.pry


    # upload_1 = FactoryBot.create(:upload)
    # upload_2 = FactoryBot.create(:upload)
    # attachment_1 = FactoryBot.create(:attachment, request_id: request.id)
    # attachment_2 = FactoryBot.create(:attachment, request_id: request.id)

    # attachment = \
    #   FactoryBot.build(:attachment,
    #                    id: '919fbdd1-111c-49b7-aeb0-2d5d8825ed00')
    # first_model.add_attachment(attachment)



    # item
    i1 = FactoryBot.create(:item,
                           leihs_model: first_model,
                           inventory_pool: @inventory_pool,
                           owner: @inventory_pool)

    # a = FactoryBot.create :attachment, item: i1
    # @attachment_filenames << a.filename
    # puts ">>> attachment: #{a.filename}"

    # add model-property
    puts ">>>>>>> first_model: #{first_model.id}"
    first_model.add_property(FactoryBot.create(:property, key: "test", value: "test"))

    # ok / entitlements
    @group_1 = FactoryBot.create(:entitlement_group, inventory_pool_id: @inventory_pool.id)
    FactoryBot.create(:entitlement, leihs_model: first_model, entitlement_group: @group_1, quantity: 2)

  end
end