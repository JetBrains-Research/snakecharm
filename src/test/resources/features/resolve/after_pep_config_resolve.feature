# Feature not implemented
@ignore
Feature: Resolve after pep.config

  Scenario Outline: Resolve at toplevel
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
    pep.config<reference_type>
    """
    When I put the caret after pep.config<caret_place>
    Then reference should resolve to "<key>" in "config.yaml"
    Examples:
      | reference_type  | caret_place | key         |
      | .custom_key1    | .           | custom_key1 |
      | .custom_key2    | .           | custom_key2 |
      | ['custom_key1'] | ['          | custom_key1 |
      | ['custom_key1'] | ['          | custom_key2 |

  Scenario Outline: Resolve to yamlfile if pep_version missing
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config<reference_type>
    """
    When I put the caret after pep.config<caret_place>
    Then reference should resolve to "custom_key1" in "config.yaml"
    Examples:
      | reference_type  | caret_place |
      | .pep_version    | .           |
      | ['pep_version'] | ['          |

  Scenario Outline: Resolve to text keys
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <text_key>: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config<reference_type>
    """
    When I put the caret after pep.config<caret_place>
    Then reference should resolve to "<text_key>" in "config.yaml"
    Examples:
      | reference_type      | caret_place | text_key        |
      | .pep_version        | .           | pep_version     |
      | .sample_table       | .           | sample_table    |
      | .subsample_table    | .           | subsample_table |
      | ['pep_version']     | ['          | pep_version     |
      | ['sample_table']    | ['          | sample_table    |
      | ['subsample_table'] | ['          | subsample_table |

  Scenario Outline: Not resolve to text keys as mapping
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <text_key>:
      key:
        value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config<reference_type>
    """
    When I put the caret after pep.config<caret_place>
    Then reference should not resolve
    Examples:
      | reference_type      | caret_place | text_key        |
      | .pep_version        | .           | pep_version     |
      | .sample_table       | .           | sample_table    |
      | .subsample_table    | .           | subsample_table |
      | ['pep_version']     | ['          | pep_version     |
      | ['sample_table']    | ['          | sample_table    |
      | ['subsample_table'] | ['          | subsample_table |

  Scenario Outline: Resolve to mapping keys
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
    pep.config<reference_type>
    """
    When I put the caret after pep.config<caret_place>
    Then reference should resolve to "<mapping_key>" in "config.yaml"
    Examples:
      | reference_type        | caret_place | mapping_key       |
      | .sample_modifiers     | .           | sample_modifiers  |
      | .project_modifiers    | .           | project_modifiers |
      | ['sample_modifiers '] | ['          | sample_modifiers  |
      | ['project_modifiers'] | ['          | project_modifiers |

  Scenario Outline: Not resolve to mapping keys as text
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    <mapping_key>: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    pep.config<reference_type>
    """
    When I put the caret after pep.config<caret_place>
    Then reference should not resolve
    Examples:
      | reference_type        | caret_place | mapping_key       |
      | .sample_modifiers     | .           | sample_modifiers  |
      | .project_modifiers    | .           | project_modifiers |
      | ['sample_modifiers '] | ['          | sample_modifiers  |
      | ['project_modifiers'] | ['          | project_modifiers |

  Scenario Outline: Resolve inside rule
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    rule Name:
      <section>:
        pep.config<reference_type>
    """
    When I put the caret after pep.config<caret_place>
    Then reference should resolve to "<key>" in "config.yaml"
    Examples:
      | reference_type   | caret_place | key         | section |
      | .custom_key1     | .           | custom_key1 | section |
      | .custom_key2     | .           | custom_key2 | run     |
      | ['custom_key1 '] | ['          | custom_key1 | section |
      | ['custom_key2']  | ['          | custom_key2 | run     |

  Scenario Outline: Resolve in "onstart" section
    Given a snakemake project
    Given I open a file "config.yaml" with text
    """
    custom_key1: value
    custom_key2: value
    """
    Given I open a file "foo.smk" with text
    """
    pepfile: "config.yaml"
    onstart :
           pep.config<reference_type>"
    """
    When I put the caret after pep.config<caret_place>
    And I invoke autocompletion popup
    Then reference should resolve to "<key>" in "config.yaml"
    Examples:
      | reference_type   | caret_place | key         |
      | .custom_key1     | .           | custom_key1 |
      | .custom_key2     | .           | custom_key2 |
      | ['custom_key1 '] | ['          | custom_key1 |
      | ['custom_key2']  | ['          | custom_key2 |

  Scenario Outline: Resolve in injections
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
           <section>: "{pep.config<reference_type>}"
    """
    When I put the caret after "{pep.config<caret_place>
    Then reference in injection should resolve to "<key>" in "config.yaml"
    Examples:
      | reference_type  | caret_place | rule_like  | section | key         |
      | .custom_key1    | .           | rule       | shell   | custom_key1 |
      | .custom_key2    | .           | rule       | message | custom_key2 |
      | .custom_key1    | .           | checkpoint | shell   | custom_key1 |
      | ['custom_key2'] | ['          | rule       | shell   | custom_key2 |
      | ['custom_key1'] | ['          | rule       | message | custom_key1 |
      | ['custom_key2'] | ['          | checkpoint | shell   | custom_key2 |

  Scenario Outline: No resolve in injections for wildcards expanding/defining sections
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
           <section>: "{pep.config<reference_type>}"
        """
    When I put the caret after "{pep.config<caret_place>
    Then reference in injection should not resolve
    Examples:
      | reference_type  | caret_place | rule_like  | section |
      | .custom_key1    | .           | rule       | input   |
      | .custom_key2    | .           | rule       | output  |
      | .custom_key1    | .           | rule       | log     |
      | .custom_key2    | .           | checkpoint | input   |
      | ['custom_key1'] | ['          | rule       | input   |
      | ['custom_key2'] | ['          | rule       | output  |
      | ['custom_key1'] | ['          | rule       | log     |
      | ['custom_key2'] | ['          | checkpoint | input   |
