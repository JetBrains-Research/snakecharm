Feature: Inspection warns about depreciated/removed keywords, or keywords that were not introduced yet.

  Scenario Outline: Using deprecated keyword
    Given a snakemake project
    And depreciation data file content is
    """
    changelog:
      - version: "1.10.01"
        deprecated:
        - name: "input"
    """
    And I set snakemake version to 1.11.11
    And I open a file "foo.smk" with text
    """
    <rule_like> foo:
        input: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection warning on <input> with message
    """
      Keyword 'input' was deprecated in version 1.10.01
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Using deprecated keyword with advice
    Given a snakemake project
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "<keyword>"
          advice: "use 'input' instead"
    """
    And I set snakemake version to 1.11.11
    And I open a file "foo.smk" with text
    """
    rule foo:
        <keyword>: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection warning on <input> with message
    """
      Keyword '<keyword>' was deprecated in version <version> - you should use 'input' instead
    """
    When I check highlighting errors
    Examples:
      | keyword | version |
      | output  | 1.11.11 |
      | shell   | 1.10.01 |

  Scenario Outline: Using removed keyword
    Given a snakemake project
    And depreciation data file content is
    """
    changelog:
      - version: "1.10.01"
        removed:
        - name: "input"
    """
    And I set snakemake version to 1.11.11
    And I open a file "foo.smk" with text
    """
    <rule_like> foo:
        input: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection error on <input> with message
    """
      Keyword 'input' was removed in version 1.10.01
    """
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Using removed keyword with advice
    Given a snakemake project
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        removed:
        - name: "<keyword>"
          advice: "use 'input' instead"
    """
    And I set snakemake version to 1.11.11
    And I open a file "foo.smk" with text
    """
    rule foo:
        <keyword>: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection error on <input> with message
    """
      Keyword '<keyword>' was removed in version <version> - you should use 'input' instead
    """
    When I check highlighting errors
    Examples:
      | keyword | version |
      | output  | 1.11.11 |
      | shell   | 1.10.01 |

  Scenario Outline: Warnings not trigger on older versions
    Given a snakemake project
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        deprecated:
        - name: "<deprecated_keyword>"
          advice: "use 'input' instead"
        removed:
        - name: "<removed_keyword>"
          advice: "use 'input' instead"
    """
    And I set snakemake version to <old_version>
    And I open a file "foo.smk" with text
    """
    rule foo:
        <removed_keyword>: "boo"
        <deprecated_keyword>: "foo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect no inspection warnings
    And I expect no inspection errors
    Examples:
      | removed_keyword | deprecated_keyword | version | old_version |
      | output          | shell              | 1.11.11 | 1.11.10     |
      | shell           | input              | 1.10.01 | 1.10.0      |

  Scenario Outline: Only the latest update is applied
    Given a snakemake project
    And depreciation data file content is
    """
    changelog:
      - version: "<version1>"
        deprecated:
        - name: "output"
          advice: "use 'input' instead"
      - version: "<version2>"
        removed:
        - name: "output"
          advice: "use 'input' instead"
    """
    And I set snakemake version to <new_version>
    And I open a file "foo.smk" with text
    """
    rule foo:
        output: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection error on <keyword> with message
    """
      Keyword 'output' was removed in version <version2>
    """
    And I expect no inspection warnings
    Examples:
      | version1 | version2 | new_version |
      | 1.11.8   | 1.11.11  | 1.11.11     |
      | 1.7.1    | 1.10.01  | 1.12.0      |

  Scenario Outline: New words introduced
    Given a snakemake project
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "localname"
    """
    And I set snakemake version to <smk_version>
    And I open a file "foo.smk" with text
    """
    rule foo:
        localname: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect inspection error on <localname> with message
    """
      Keyword 'localname' was added in version <version>, but selected Snakemake version is <smk_version>
    """
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.10     |
      | 1.10.01 | 1.9.0       |

  Scenario Outline: New words error does not appear when version is correct
    Given a snakemake project
    And depreciation data file content is
    """
    changelog:
      - version: "<version>"
        introduced:
        - name: "localname"
    """
    And I set snakemake version to <smk_version>
    And I open a file "foo.smk" with text
    """
    rule foo:
        localname: "boo"
    """
    When SmkDepreciatedKeywords inspection is enabled
    Then I expect no inspection errors
    Examples:
      | version | smk_version |
      | 1.11.11 | 1.11.11     |
      | 1.10.01 | 1.10.21     |
