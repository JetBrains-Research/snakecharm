Feature: Completion after pep object

  Scenario: Complete at toplevel
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    pep.
    """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list should contain:
      | __init__                   |
      | create_samples             |
      | _reinit                    |
      | _get_table_from_samples    |
      | parse_config_file          |
      | load_samples               |
      | modify_samples             |
      | _modifier_exists           |
      | attr_remove                |
      | attr_constants             |
      | attr_synonyms              |
      | _assert_samples_have_names |
      | attr_merge                 |
      | attr_imply                 |
      | attr_derive                |
      | activate_amendments        |
      | deactivate_amendments      |
      | add_samples                |
      | infer_name                 |
      | get_description            |
      | __str__                    |
      | amendments                 |
      | list_amendments            |
      | config                     |
      | config_file                |
      | samples                    |
      | sample_name_colname        |
      | sample_table               |
      | subsample_table            |
      | _read_sample_data          |
      | _get_cfg_v                 |
      | _format_cfg                |
      | get_sample                 |
      | get_samples                |

  Scenario: Complete inside rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule Name:
      section:
        pep.
    """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list should contain:
      | __init__       |
      | create_samples |
      | _reinit        |

  Scenario Outline: Complete in injections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
           <section>: "{pep.}"
    """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list should contain:
      | __init__   |
      | config     |
      | get_sample |
    Examples:
      | rule_like  | section |
      | rule       | shell   |
      | rule       | message |
      | checkpoint | shell   |

  Scenario Outline: No completion in injections for wildcards expanding/defining sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{pep.}"
        """
    When I put the caret after pep.
    And I invoke autocompletion popup
    Then completion list shouldn't contain:
      | __init__       |
      | create_samples |
      | _reinit        |

    Examples:
      | rule_like  | section |
      | rule       | input   |
      | rule       | output  |
      | rule       | log     |
      | checkpoint | input   |

  Scenario: Complete in not-empty context
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     pep.c
     """
    When I put the caret after pep.c
    And I invoke autocompletion popup
    Then completion list should contain:
      | config |

  Scenario Outline: Parenthesis inserted after method completion
    Given a snakemake project
    Given I open a file "foo.smk" with text
      """
      pep.
      """
    When I put the caret after pep.
    Then I invoke autocompletion popup, select "<item>" lookup item and see a text:
      """
      pep.<inserted_text>
      """
    Examples:
      | item        | inserted_text |
      | config      | config        |
      | get_samples | get_samples() |