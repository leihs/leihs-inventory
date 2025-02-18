def validate_map_structure(map, required_keys)

  errors = []

  required_keys.each do |key, expected_classes|
    unless map.key?(key)
      errors << "Missing key: #{key}"
      next
    end

    value = map[key]
    expected_classes = Array(expected_classes)

    unless expected_classes.any? { |cls| value.is_a?(cls) }
      errors << "Invalid type for key '#{key}': expected #{expected_classes.join(' or ')}, got #{value.class}"
    end
  end

  if errors.empty?
    puts "Map structure is valid."
    true
  else
    puts "Validation failed with errors:"
    errors.each { |error| puts "- #{error}" }
    false
  end
end