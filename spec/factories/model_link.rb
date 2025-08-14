class ModelLink < Sequel::Model(:model_links)
  plugin :validation_helpers

  many_to_one :model_group, class: :Category     # FK: model_group_id
  many_to_one :model, class: :LeihsModel       # FK: model_id

  def before_validation
    super
    self.quantity ||= 1
  end

  # prevent duplicated model in Category, but allow for Template
  def validate
    super
    validates_presence [:model_group_id, :model_id]
    validates_integer :quantity

    # if model_group && model_group.is_a?(Category)
    #   validates_unique [:model_id, :model_group_id],
    #     message: _("already in Category")
    # end
  end
end
