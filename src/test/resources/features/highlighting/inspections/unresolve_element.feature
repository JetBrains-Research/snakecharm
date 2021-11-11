Feature: Inspection: Unresolved element
  Mostly checks that python inspection is enabled for our unresolved references

  Scenario Outline: Unresolved rule in localrules or ruleorder
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     <section>: NAME
     """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection error on <NAME> with message
     """
     Cannot find rule or checkpoint 'NAME'
     """
    When I check highlighting errors ignoring extra highlighting
    Examples:
      | section    |
      | localrules |
      | ruleorder  |

  Scenario Outline: Unresolved rule after rules.
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <opposite_rule_like> NAME:
      input: ""

    <rule_like> ANOTHER:
      input: <rule_like>s.NAME
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection error on <NAME> in <<rule_like>s.NAME> with message
    """
    Cannot find reference 'NAME' in '<rule_like>s'
    """
    When I check highlighting errors ignoring extra highlighting
    Examples:
      | rule_like  | opposite_rule_like |
      | rule       | subworkflow        |
      | checkpoint | rule               |
      | checkpoint | subworkflow        |

  Scenario Outline: Unresolved wildcard after wildcards in injection
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      <section>: "{wildcards.sample}"
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection warning on <sample> with message
    """
    Cannot find reference 'sample' in 'Rule 'foo' wildcards'
    """
    When I check highlighting warnings ignoring extra highlighting
    Examples:
      | rule_like  | section |
      | rule       | shell   |
      | rule       | message |
      | checkpoint | shell   |


  Scenario Outline: Unresolved wildcard in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      run:
        print(wildcards.sample)
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection warning on <sample> with message
    """
    Cannot find reference 'sample' in 'Rule 'foo' wildcards'
    """
    When I check highlighting warnings ignoring extra highlighting
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: Synthetic jobid variable not marked as unresolved in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      run:
        jobid
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario: Unresolved variable in injection
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule foo:
      shell: "{dooooo}"
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection warning on <dooooo> with message
    """
    Unresolved reference 'dooooo'
    """
    When I check highlighting warnings ignoring extra highlighting

  Scenario: Unresolved variable in run section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule foo:
      run:
        print(dooooo)
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection error on <dooooo> with message
    """
    Unresolved reference 'dooooo'
    """
    When I check highlighting errors ignoring extra highlighting

  Scenario: Unresolved conda path
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     rule:
       conda: "boo.yaml"
     """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect inspection error on <boo.yaml> with message
     """
     Unresolved reference 'boo.yaml'
     """
    When I check highlighting errors

  # Ignore unresolved conda env path if it cannot be statically analyzed #256
  Scenario: Unresolved conda path (complex string)
    Given a snakemake project
    Given I open a file "foo.smk" with text
     """
     rule:
       conda: f"{2}/boo.yaml"
     """
    And PyUnresolvedReferencesInspection inspection is enabled
    # PyUnresolvedReferencesInspection replaces it with: LIKE_UNKNOWN_SYMBOL severity
    Then I expect inspection warning on <{2}/boo.yaml> with message
     """
     Unresolved reference '{2}/boo.yaml'
     """
    When I check highlighting warnings

  Scenario Outline: Implicit variables not marked as unresolved
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <object>
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors
    Examples:
      | object |
      | pep    |
      | config |

