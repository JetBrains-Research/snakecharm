Feature: Inspection warns about depreciated/removed keywords, or keywords that were not introduced yet.

  Scenario Outline: Using deprecated subsection keyword
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "1.10.01"
        deprecated:
        - name: "input"
          type: "<rule_like>"
    """
    And I set snakemake language version to "1.11.11"
    And I open a file "foo.smk" with text
    """
    <rule_like> foo:
        input: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection weak warning on <input> with message
    """
    Usage of 'input' in '<rule_like>' was deprecated in version 1.10.01
    """
    When I check highlighting weak warnings
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Deprecation does not affect other sections
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "1.10.01"
        deprecated:
        - name: "input"
          type: "rule"
    """
    Given I set snakemake language version to "1.11.11"
    And I open a file "foo.smk" with text
    """
    checkpoint foo:
        input: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      |  |


  Scenario Outline: Removal does affect only selected sections
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "1.10.01"
        deprecated:
        - name: "input"
          type: "rule"
      - version: "1.10.02"
        removed:
        - name: "input"
          type: "rule"
    """
    And I set snakemake language version to "1.11.11"
    And I open a file "foo.smk" with text
    """
    rule boo:
        input: "foo"
    checkpoint foo:
        input: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <input> with message
    """
    Usage of 'input' in 'rule' was removed in version 1.10.02
    """
    When I check highlighting errors
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      |  |


  Scenario Outline: Using deprecated subsection keyword with advice
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "output"
          type: "rule-like"
          advice: "use 'input' instead"
    """
    And I set snakemake language version to "1.11.11"
    And I open a file "foo.smk" with text
    """
    rule foo:
        output: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection weak warning on <output> with message
    """
    Usage of 'output' in 'rule' was deprecated in version <version> - you should use 'input' instead
    """
    When I check highlighting weak warnings
    Examples:
      | version |
      | 1.11.11 |
      | 1.10.01 |

  Scenario Outline: Using deprecated top-level keyword with advice
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "wildcard_constraints"
          type: "top-level"
          advice: "use 'input' instead"
    """
    And I set snakemake language version to "1.11.11"
    And I open a file "foo.smk" with text
    """
    wildcard_constraints: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection weak warning on <wildcard_constraints> with message
    """
    Top level directive 'wildcard_constraints' was deprecated in version <version> - you should use 'input' instead
    """
    When I check highlighting weak warnings
    Examples:
      | version |
      | 1.11.11 |
      | 1.10.01 |

  Scenario Outline: Using removed subsection keyword
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "1.10.01"
        removed:
        - name: "input"
          type: "<rule_like>"
    """
    And I set snakemake language version to "1.11.11"
    And I open a file "foo.smk" with text
    """
    <rule_like> foo:
        input: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <input> with message
    """
    Usage of 'input' in '<rule_like>' was removed in version 1.10.01
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Using removed subsection keyword with advice
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        removed:
        - name: "shell"
          type: "rule"
          advice: "use 'input' instead"
    """
    And I set snakemake language version to "1.11.11"
    And I open a file "foo.smk" with text
    """
    rule foo:
        shell: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <shell> with message
    """
    Usage of 'shell' in 'rule' was removed in version <version> - you should use 'input' instead
    """
    When I check highlighting errors
    Examples:
      | version |
      | 1.11.11 |
      | 1.10.01 |

  Scenario Outline: Using removed top-level keyword with advice
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        removed:
        - name: "wildcard_constraints"
          type: "top-level"
          advice: "use 'input' instead"
    """
    And I set snakemake language version to "1.11.11"
    And I open a file "foo.smk" with text
    """
    wildcard_constraints: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <wildcard_constraints> with message
    """
    Top level directive 'wildcard_constraints' was removed in version <version> - you should use 'input' instead
    """
    When I check highlighting errors
    Examples:
      | version |
      | 1.11.11 |
      | 1.10.01 |

  Scenario Outline: Warnings not trigger on older versions
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "<deprecated_keyword>"
          advice: "use 'input' instead"
          type: "subsection"
        removed:
        - name: "<removed_keyword>"
          advice: "use 'input' instead"
          type: "subsection"
    """
    And I set snakemake language version to "<old_version>"
    And I open a file "foo.smk" with text
    """
    rule foo:
        <removed_keyword>: "boo"
        <deprecated_keyword>: "foo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    And I expect no inspection errors
    When I check highlighting errors
    Examples:
      | removed_keyword | deprecated_keyword | version | old_version |
      | output          | shell              | 1.11.11 | 1.11.10     |
      | shell           | input              | 1.10.01 | 1.10.0      |

  Scenario Outline: Only the latest update is applied
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version1>"
        deprecated:
        - name: "output"
          advice: "use 'input' instead"
          type: "rule-like"
      - version: "<version2>"
        removed:
        - name: "output"
          advice: "use 'input' instead"
          type: "rule-like"
    """
    And I set snakemake language version to "<new_version>"
    And I open a file "foo.smk" with text
    """
    rule foo:
        output: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    And I expect no inspection weak warnings
    When I check highlighting weak warnings ignoring extra highlighting
    Then I expect inspection error on <output> with message
    """
    Usage of 'output' in 'rule' was removed in version <version2> - you should use 'input' instead
    """
    When I check highlighting errors
    Examples:
      | version1 | version2 | new_version |
      | 1.11.8   | 1.11.11  | 1.11.11     |
      | 1.7.1    | 1.10.01  | 1.12.0      |

  Scenario Outline: New subsection keywords introduced
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "localname"
          type: "<rule_like>"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    <section> foo:
        localname: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <localname> with message
    """
    Usage of 'localname' in '<section>' was added in version <version>, but selected Snakemake version is <smk_version>
    """
    When I check highlighting errors
    Examples:
      | version | smk_version | rule_like  | section    |
      | 1.11.11 | 1.11.10     | rule-like  | rule       |
      | 1.11.11 | 1.11.10     | rule-like  | checkpoint |
      | 1.11.11 | 1.11.10     | rule       | rule       |
      | 1.11.11 | 1.11.10     | checkpoint | checkpoint |
      | 1.10.01 | 1.9.0       | rule-like  | rule       |
      | 1.10.01 | 1.9.0       | rule-like  | checkpoint |

  Scenario Outline: Subsection can be deprecated from multiple top level directives
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "localname"
          type: "rule-like"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    rule foo:
        localname: "boo"
    checkpoint boo:
        localname: "foo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection weak warning on <localname> in <localname: "boo"> with message
    """
    Usage of 'localname' in 'rule' was deprecated in version <version>
    """
    Then I expect inspection weak warning on <localname> in <localname: "foo"> with message
    """
    Usage of 'localname' in 'checkpoint' was deprecated in version <version>
    """
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.13.0      |

  Scenario Outline: Subsection can be deprecated from everywhere
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "localname"
          type: "rule-like"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    rule foo:
        localname: "foo"
    checkpoint coo:
        localname: "coo"
    use rule boo as uoo with:
        localname: "uoo"
    module moo:
        localname: "moo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection weak warning on <localname> in <localname: "foo"> with message
    """
    Usage of 'localname' in 'rule' was deprecated in version <version>
    """
    Then I expect inspection weak warning on <localname> in <localname: "coo"> with message
    """
    Usage of 'localname' in 'checkpoint' was deprecated in version <version>
    """
    Then I expect inspection weak warning on <localname> in <localname: "uoo"> with message
    """
   Usage of 'localname' in 'use' was deprecated in version <version>
    """
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.13.0      |

  Scenario Outline: New subsection keywords error does not appear when version is correct
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "localname"
          type: "subsection"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    rule foo:
        localname: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |


  Scenario Outline: Functions can be deprecated
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "<fqn>"
          type: "function"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    def fooboo_local_fun():
        pass

    rule foo:
        params: "p"
        output: <short_name>("")
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Then I expect inspection weak warning on <<short_name>> in <<short_name>("")> with message
    """
    Function '<fqn>' was deprecated in version <version>
    """
    When I check highlighting weak warnings
    Examples:
      | version | smk_version | fqn                  | short_name       |
      | 1.11.11 | 1.11.11     | snakemake.io.expand  | expand           |
      | 1.10.01 | 1.10.21     | snakemake.io.expand  | expand           |
      | 1.11.11 | 1.11.11     | foo.fooboo_local_fun | fooboo_local_fun |

  Scenario Outline: Unresolved functions can be deprecated
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        removed:
        - name: "<fqn>"
          type: "function"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    rule foo:
        output: <short_name>("")
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <<short_name>> with message
    """
    Unresolved function '<short_name>' looks like '<fqn>' that was removed in version <version>
    """
    When I check highlighting errors
    Examples:
      | version | smk_version | fqn                 | short_name |
      | 1.11.11 | 1.11.11     | snakemake.io.fooboo | fooboo     |
      | 1.11.11 | 2.11.11     | snakemake.io.fooboo | fooboo     |

  Scenario Outline: Functions can be deprecated with advice
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "snakemake.io.expand"
          type: "function"
          advice: "use 'expand2' instead"

    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    rule foo:
        output: expand("")
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection weak warning on <expand> with message
    """
    Function 'snakemake.io.expand' was deprecated in version <version> - you should use 'expand2' instead
    """
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |

  Scenario Outline: Top level directives can be deprecated
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "subworkflow"
          type: "top-level"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    subworkflow foo:
       snakefile: "bar"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection weak warning on <subworkflow> with message
    """
    Top level directive 'subworkflow' was deprecated in version <version>
    """
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |


  Scenario Outline: Top level directives can be removed
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        removed:
        - name: "subworkflow"
          type: "top-level"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    subworkflow foo:
       snakefile: "bar"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <subworkflow> with message
    """
    Top level directive 'subworkflow' was removed in version <version>
    """
    When I check highlighting errors
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |


  Scenario Outline: Top level directives can be deprecated with advice
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "subworkflow"
          type: "top-level"
          advice: "use 'module' instead"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    subworkflow foo:
       snakefile: "bar"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection weak warning on <subworkflow> with message
    """
    Top level directive 'subworkflow' was deprecated in version <version> - you should use 'module' instead
    """
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |


  Scenario Outline: Top level directives don't affect subdirectives
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "config"
          type: "top-level"
          advice: "use 'module' instead"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    module:
      config: ""
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |

  Scenario Outline: New top level directive error appears when version is earlier that requested
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "module"
          type: "top-level"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    module foo:
        snakefile: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <module> with message
    """
    Top level directive 'module' was added in version <version>, but selected Snakemake version is <smk_version>
    """
    When I check highlighting errors
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.10     |
      | 1.10.01 | 1.10.00     |

  Scenario Outline: New top level directive error does not appear when version is correct
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "module"
          type: "top-level"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    module foo:
        snakefile: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |

  Scenario Outline: Default version can be defined in file
    Given a snakemake project
    And snakemake framework api yaml descriptor is
    """
    defaultVersion: "<smk_version>"
    changelog:
      - version: "<version>"
        introduced:
        - name: "localname"
          type: "rule-like"
    """
    And I set snakemake language version to "<smk_version>"
    And I open a file "foo.smk" with text
    """
    rule foo:
        localname: "boo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <localname> with message
    """
    Usage of 'localname' in 'rule' was added in version <version>, but selected Snakemake version is <smk_version>
    """
    When I check highlighting errors
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.10     |
      | 1.10.01 | 1.9.0       |

    Scenario Outline: Show error when 'use' section contains non-available execution subsections configured using YAML
      Given a snakemake project
      And snakemake framework api yaml descriptor is
    """
    changelog:
      - version: "3.0.0"
        override:
        - name: "foobooo"
          type: "rule-like"
          execution_section: False

      - version: "2.0.0"
        introduced:
        - name: "foobooo"
          type: "rule-like"
          execution_section: True

        - name: "threads"
          type: "rule-like"
    """
    And I set snakemake language version to "<lang_version>"
    Given I open a file "foo.smk" with text
    """
    use rule RULE as NEW_RULE with:
        input: "file1"
        output: "file2"
        foobooo: "foo"
    """
    When SmkDepreciatedKeywordsInspection inspection is enabled
    Then I expect inspection error on <foobooo> with message
    """
    Usage of 'foobooo' in 'use' was added in version 2.0.0, but selected Snakemake version is <lang_version>
    """
    When I check highlighting errors
    Examples:
        | lang_version |
        | 1.0.0        |