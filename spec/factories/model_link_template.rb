class ModelLinkTemplate < Sequel::Model(:model_links)
  plugin :validation_helpers

  many_to_one :model_group, class: :Template
  many_to_one :model, class: :LeihsModel

  def before_validation
    super
    self.quantity ||= 1
  end

  def validate
    super
    validates_presence [:model_group_id, :model_id]
    validates_integer :quantity
  end
end
