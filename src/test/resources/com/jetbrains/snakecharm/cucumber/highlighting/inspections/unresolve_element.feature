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

#  Scenario: Unresolved simple wildcard in injection
#    Given TODO
#
#  Scenario Outline: Unresolved wildcard after wildcards in injection
#    Given a snakemake project
#    Given I open a file "foo.smk" with text
#    """
#    <rule_like> foo:
#      <section>: "{wildcards.sample}"
#    """
#    And PyUnresolvedReferencesInspection inspection is enabled
#    Then I expect inspection error on <sample> with message
#    """
#    Cannot find reference 'sample' in 'wildcards'
#    """
#    When I check highlighting warnings ignoring extra highlighting
#    Examples:
#      | rule_like  | section |
#      | rule       | shell   |
#      | rule       | message |
#      | checkpoint | shell   |
#
#
#  Scenario: Unresolved wildcard in run section
#    Given TODO
#
#  Scenario: Unresolved section name in injection
#    Given TODO
#
#  Scenario: Unresolved section name in run section
#    Given TODO
#
#  Scenario: Unresolved variable in injection
#    Given TODO
