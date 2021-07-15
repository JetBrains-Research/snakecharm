Feature: SOE, issue 380
  Scenario: Foo
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def foo():
        rule:
            shell:  "{config[methtool][proportion]}"
        pass
    """
    When I put the caret after propo
    Then I expect language injection on "{config[methtool][proportion]}"
    And validate issue 380