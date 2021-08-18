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
      | property            |
      | amendments          |
      | list_amendments     |
      | config              |
      | config_file         |
      | samples             |
      | sample_name_colname |
      | sample_table        |
      | subsample_table     |
      | get_sample          |
      | get_samples         |

  Scenario Outline: Resolve inside rule
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule Name:
      <section>:
        pep.<property>
    """
    When I put the caret after pep.
    Then reference should resolve to "<property>" in "project.py"
    Examples:
      | property            | section |
      | config_file         | section |
      | samples             | run     |
      | sample_name_colname | onstart |

  Scenario Outline: Resolve in injections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
           <section>: "{pep.<property>}"
    """
    When I put the caret after "{pep.
    Then reference in injection should resolve to "<property>" in "project.py"
    Examples:
      | rule_like  | section | property            |
      | rule       | shell   | config_file         |
      | rule       | message | samples             |
      | checkpoint | shell   | sample_name_colname |

  Scenario Outline: No resolve in injections for wildcards expanding/defining sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
        """
        <rule_like> NAME:
           <section>: "{pep.<property>}"
        """
    When I put the caret after "{pep.
    Then reference in injection should not resolve
    Examples:
      | rule_like  | section | property            |
      | rule       | input   | config_file         |
      | rule       | output  | samples             |
      | rule       | log     | sample_name_colname |
      | checkpoint | input   | amendments          |

