Feature: Inspection for sections with fixed completion variants

  Scenario Outline: Unknown shadow setting or typo in setting name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        output: "output.txt"
        <section>: "parameter"
    """
    And <inspection> inspection is enabled
    Then I expect inspection warning on <"parameter"> with message
    """
    <msg>
    """
    When I check highlighting warnings
    Examples:
      | section         | inspection                          | msg                                                                                                 |
      | shadow          | SmkShadowSettingsInspection         | Shadow must either be 'shallow', 'full', 'minimal', 'copy-minimal', or True (equivalent to 'full'). |
      | template_engine | SmkTemplateEngineSettingsInspection | Supported template engines are: 'jinja2', 'yte'                                                     |

  Scenario Outline: No inspection if supported setting name
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        output: "output.txt"
        <section>: "<option>"
    """
    And <inspection> inspection is enabled
    And SmkSectionMultipleArgsInspection inspection is enabled
    Then I expect no inspection warnings
    When I check highlighting warnings
    Examples:
      | section         | inspection                          | option |
      | shadow          | SmkShadowSettingsInspection         | full   |
      | template_engine | SmkTemplateEngineSettingsInspection | jinja2 |
