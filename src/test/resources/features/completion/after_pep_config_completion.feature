Feature: Completion after pep.config
  # TODO Feature not implemented
  @ignore
  Scenario Outline: Complete at toplevel
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      custom_key3:
        value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |
    Examples:
      | complete_type  | caret_place  |
      | pep.config.    | pep.config.  |
      | pep.config[''] | pep.config[' |

  # TODO Feature not implemented
  @ignore
  Scenario: Not complete no variable keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1.custom_key2: value
    custom_key3:custom_key4:
        custom_key5:
          value
    custom_key6: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config.
    """
    When I put the caret after pep.config.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | custom_key1.custom_key2 |
      | custom_key3:custom_key4 |
      | custom_key1             |
      | custom_key3             |
    
  # TODO Feature not implemented
  @ignore
  Scenario: Complete no variable keys in subscription form
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1.custom_key2: value
    custom_key3:custom_key4:
        custom_key5:
          value
    custom_key6: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config['']
    """
    When I put the caret after pep.config['
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1.custom_key2 |
      | custom_key3:custom_key4 |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: Complete pep_version
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    pep_version: "2.0.0"
    custom_key: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list should contain:
      | pep_version |
      | custom_key  |
    Examples:
      | complete_type  | caret_place  |
      | pep.config.    | pep.config.  |
      | pep.config[''] | pep.config[' |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: Complete pep_version should be even if the section is missing
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list should contain:
      | pep_version |
      | custom_key  |
    Examples:
      | complete_type  | caret_place  |
      | pep.config.    | pep.config.  |
      | pep.config[''] | pep.config[' |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: Complete text keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <text_key>: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <text_key>  |
      | pep_version |
    Examples:
      | complete_type  | caret_place  | text_key        |
      | pep.config.    | pep.config.  | sample_table    |
      | pep.config.    | pep.config.  | subsample_table |
      | pep.config[''] | pep.config[' | sample_table |
      | pep.config[''] | pep.config[' | subsample_table |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: Not complete text keys as mapping
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <text_key>:
      key1:
        value
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | <text_key> |
      | custom_key |
    Examples:
      | complete_type  | caret_place  | text_key        |
      | pep.config.    | pep.config.  | pep_version     |
      | pep.config.    | pep.config.  | sample_table    |
      | pep.config.    | pep.config.  | subsample_table |
      | pep.config[''] | pep.config[' | pep_version     |
      | pep.config[''] | pep.config[' | sample_table    |
      | pep.config[''] | pep.config[' | subsample_table |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: Complete mapping keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <mapping_key>:
      custom_key2:
        value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list should contain:
      | <mapping_key> |
      | pep_version   |
    Examples:
      | complete_type  | caret_place  | mapping_key       |
      | pep.config.    | pep.config.  | sample_modifiers  |
      | pep.config.    | pep.config.  | project_modifiers |
      | pep.config[''] | pep.config[' | sample_modifiers  |
      | pep.config[''] | pep.config[' | project_modifiers |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: Not complete mapping keys as text
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <mapping_key>: value
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | <mapping_key> |
    Examples:
      | complete_type  | caret_place  | mapping_key       |
      | pep.config.    | pep.config.  | sample_modifiers  |
      | pep.config.    | pep.config.  | project_modifiers |
      | pep.config[''] | pep.config[' | sample_modifiers  |
      | pep.config[''] | pep.config[' | project_modifiers |


  # TODO Feature not implemented
  @ignore
  Scenario Outline: Complete inside rule
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      key:
        value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    rule Name:
      <section>:
        <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |
      | pep_version |
    Examples:
      | complete_type  | caret_place  | section |
      | pep.config.    | pep.config.  | section |
      | pep.config.    | pep.config.  | run     |
      | pep.config[''] | pep.config[' | section |
      | pep.config[''] | pep.config[' | run     |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: Complete in injections
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      key:
        value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <rule_like> NAME:
           <section>: "{<complete_type>}"
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |
      | pep_version |
    Examples:
      | complete_type  | caret_place  | rule_like  | section |
      | pep.config.    | pep.config.  | rule       | shell   |
      | pep.config.    | pep.config.  | rule       | message |
      | pep.config.    | pep.config.  | checkpoint | shell   |
      | pep.config[''] | pep.config[' | rule       | shell   |
      | pep.config[''] | pep.config[' | rule       | message |
      | pep.config[''] | pep.config[' | checkpoint | shell   |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: Complete in "onstart" section
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      key:
        value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    onstart :
       <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |
    Examples:
      | complete_type  | caret_place  |
      | pep.config.    | pep.config.  |
      | pep.config[''] | pep.config[' |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: No completion in injections for wildcards expanding/defining sections
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2:
      key:
        value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
        <rule_like> NAME:
           <section>: "{<complete_type>}"
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | custom_key1 |
      | custom_key2 |
    Examples:
      | complete_type  | caret_place  | rule_like  | section |
      | pep.config.    | pep.config.  | rule       | input   |
      | pep.config.    | pep.config.  | rule       | output  |
      | pep.config.    | pep.config.  | rule       | log     |
      | pep.config.    | pep.config.  | checkpoint | input   |
      | pep.config[''] | pep.config[' | rule       | input   |
      | pep.config[''] | pep.config[' | rule       | output  |
      | pep.config[''] | pep.config[' | rule       | log     |
      | pep.config[''] | pep.config[' | checkpoint | input   |

  # TODO Feature not implemented
  @ignore
  Scenario Outline: Complete in not-empty context
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    <complete_type>
    """
    When I put the caret after <caret_place>
    And I invoke autocompletion popup
    Then completion list should contain:
      | custom_key1 |
      | custom_key2 |
    Examples:
      | complete_type   | caret_place   |
      | pep.config.c    | pep.config.c  |
      | pep.config['c'] | pep.config['c |