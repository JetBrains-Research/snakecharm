Feature: Inspection warns about depreciated/removed keywords, or keywords that were not introduced yet.

  Scenario Outline: Using deprecated subsection keyword
    Given a snakemake project
    And I set snakemake version to "1.11.11"
    And depreciation data file content is
    """
    changelog:
      - version: "1.10.01"
        deprecated:
        - name: "input"
          type: "subsection"
          parent:
            - "<rule_like>"
    """
    And I open a file "foo.smk" with text
    """
    <rule_like> foo:
        input: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
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
    Given I set snakemake version to "1.11.11"
    And depreciation data file content is
    """
    changelog:
      - version: "1.10.01"
        deprecated:
        - name: "input"
          type: "subsection"
          parent:
            - "rule"
    """
    And I open a file "foo.smk" with text
    """
    checkpoint foo:
        input: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      |  |


  Scenario Outline: Removal does affect only selected sections
    Given a snakemake project
    And I set snakemake version to "1.11.11"
    And depreciation data file content is
    """
    changelog:
      - version: "1.10.01"
        deprecated:
        - name: "input"
          type: "subsection"
          parent:
            - "rule"
      - version: "1.10.02"
        removed:
        - name: "input"
          type: "subsection"
          parent:
            - "rule"
    """
    And I open a file "foo.smk" with text
    """
    rule boo:
        input: "foo"
    checkpoint foo:
        input: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
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
    And I set snakemake version to "1.11.11"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "output"
          type: "subsection"
          advice: "use 'input' instead"
    """
    And I open a file "foo.smk" with text
    """
    rule foo:
        output: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection weak warning on <output> with message
    """
    Usage of 'output' was deprecated in version <version> - you should use 'input' instead
    """
    When I check highlighting weak warnings
    Examples:
      | version |
      | 1.11.11 |
      | 1.10.01 |

  Scenario Outline: Using removed subsection keyword
    Given a snakemake project
    And I set snakemake version to "1.11.11"
    And depreciation data file content is
    """
    changelog:
      - version: "1.10.01"
        removed:
        - name: "input"
          type: "subsection"
          parent:
            - "<rule_like>"
    """
    And I open a file "foo.smk" with text
    """
    <rule_like> foo:
        input: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
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
    And I set snakemake version to "1.11.11"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        removed:
        - name: "shell"
          type: "subsection"
          advice: "use 'input' instead"
    """
    And I open a file "foo.smk" with text
    """
    rule foo:
        shell: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection error on <shell> with message
    """
    Usage of 'shell' was removed in version <version> - you should use 'input' instead
    """
    When I check highlighting errors
    Examples:
      | version |
      | 1.11.11 |
      | 1.10.01 |

  Scenario Outline: Warnings not trigger on older versions
    Given a snakemake project
    And I set snakemake version to "<old_version>"
    And depreciation data file content is
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
    And I open a file "foo.smk" with text
    """
    rule foo:
        <removed_keyword>: "boo"
        <deprecated_keyword>: "foo"
    """
    When SmkDepreciatedKeywords inspection is enabled
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
    And I set snakemake version to "<new_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version1>"
        deprecated:
        - name: "output"
          advice: "use 'input' instead"
          type: "subsection"
      - version: "<version2>"
        removed:
        - name: "output"
          advice: "use 'input' instead"
          type: "subsection"
    """
    And I open a file "foo.smk" with text
    """
    rule foo:
        output: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    And I expect no inspection weak warnings
    When I check highlighting weak warnings ignoring extra highlighting
    Then I expect inspection error on <output> with message
    """
    Usage of 'output' was removed in version <version2> - you should use 'input' instead
    """
    When I check highlighting errors
    Examples:
      | version1 | version2 | new_version |
      | 1.11.8   | 1.11.11  | 1.11.11     |
      | 1.7.1    | 1.10.01  | 1.12.0      |

  Scenario Outline: New subsection keywords introduced
    Given a snakemake project
    And I set snakemake version to "<smk_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "localname"
          type: "subsection"
          parent:
            - "checkpoint"
            - "rule"
    """
    And I open a file "foo.smk" with text
    """
    rule foo:
        localname: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection error on <localname> with message
    """
    Usage of 'localname' in 'rule' was added in version <version>, but selected Snakemake version is <smk_version>
    """
    When I check highlighting errors
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.10     |
      | 1.10.01 | 1.9.0       |

  Scenario Outline: New subsection keywords error does not appear when version is correct
    Given a snakemake project
    And I set snakemake version to "<smk_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "localname"
          type: "subsection"
    """
    And I open a file "foo.smk" with text
    """
    rule foo:
        localname: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |


  Scenario Outline: Functions can be deprecated
    Given a snakemake project
    And I set snakemake version to "<smk_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "expand"
          type: "function"
    """
    And I open a file "foo.smk" with text
    """
    rule foo:
        output: expand("")
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Then I expect inspection weak warning on <expand> with message
    """
    Function 'expand' was deprecated in version <version>
    """
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |


  Scenario Outline: Functions can be deprecated with advice
    Given a snakemake project
    And I set snakemake version to "<smk_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "expand"
          type: "function"
          advice: "use 'expand2' instead"

    """
    And I open a file "foo.smk" with text
    """
    rule foo:
        output: expand("")
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection weak warning on <expand> with message
    """
    Function 'expand' was deprecated in version <version> - you should use 'expand2' instead
    """
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |

  Scenario Outline: Top level directives can be deprecated
    Given a snakemake project
    And I set snakemake version to "<smk_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "subworkflow"
          type: "top-level"
    """
    And I open a file "foo.smk" with text
    """
    subworkflow foo:
       snakefile: "bar"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection weak warning on <subworkflow> with message
    """
    Top level directive 'subworkflow' was deprecated in version <version>
    """
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |


  Scenario Outline: Top level directives can be deprecated with advice
    Given a snakemake project
    And I set snakemake version to "<smk_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "subworkflow"
          type: "top-level"
          advice: "use 'module' instead"
    """
    And I open a file "foo.smk" with text
    """
    subworkflow foo:
       snakefile: "bar"
    """
    When SmkDepreciatedKeywords inspection is enabled
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
    And I set snakemake version to "<smk_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "config"
          type: "top-level"
          advice: "use 'module' instead"
    """
    And I open a file "foo.smk" with text
    """
    module:
      config: ""
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect no inspection weak warnings
    When I check highlighting weak warnings
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |

  Scenario Outline: New top level directive error appears when version is earlier that requested
    Given a snakemake project
    And I set snakemake version to "<smk_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "module"
          type: "top-level"
    """
    And I open a file "foo.smk" with text
    """
    module foo:
        snakefile: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
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
    And I set snakemake version to "<smk_version>"
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "module"
          type: "top-level"
    """
    And I open a file "foo.smk" with text
    """
    module foo:
        snakefile: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |