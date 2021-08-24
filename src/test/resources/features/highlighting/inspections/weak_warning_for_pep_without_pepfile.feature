Feature: Weak warning for pep.config without config file
  Scenario: Weak warning if no config for pep.config
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    a = pep.config.custom_key3
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection warning on <custom_key3> in <pep.config.custom_key3> with message
      """
      Cannot find reference 'custom_key3' in 'pep.config'
      """
    When I check highlighting warnings
