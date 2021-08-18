Feature: Resolve after pep object

  Scenario Outline: Resolve at toplevel
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    pep.<property>
    """
    When I put the caret after pep.
    Then reference should resolve to "<property>" in "project.py"

    Examples:
      | property                   |
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

  Scenario Outline: Resolve inside rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule Name:
      section:
        pep.<property>
    """
    When I put the caret after pep.
    Then reference should resolve to "<property>" in "project.py"
    Examples:
      | property       |
      | __init__       |
      | create_samples |
      | _reinit        |

  Scenario Outline: Resolve in injections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
           <section>: "{pep.<property>}"
    """
    When I put the caret after pep.
    Then reference should resolve to "<property>" in "project.py"
    Examples:
      | rule_like  | section | property   |
      | rule       | shell   | __init__   |
      | rule       | message | config     |
      | checkpoint | shell   | get_sample |

  Scenario Outline: No resolve in injections for wildcards expanding/defining sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{pep.<property>}"
        """
    When I put the caret after pep.
    Then reference should not resolve
    Examples:
      | rule_like  | section | property                |
      | rule       | input   | __init__                |
      | rule       | output  | config                  |
      | rule       | log     | get_sample              |
      | checkpoint | input   | _get_table_from_samples |

