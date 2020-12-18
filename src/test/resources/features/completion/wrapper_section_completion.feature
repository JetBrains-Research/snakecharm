Feature: Completion for wrapper name

  Scenario Outline: Complete wrapper name for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "0.64.0/<short_name>"
    """
    And add snakemake facet with wrappers loaded
    When I put the caret after <short_name>
    And I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      wrapper: "0.64.0/<full_name>"
    """
    Examples:
      | rule_like  | short_name         | full_name                       |
      | rule       | bismark2report     | bio/bismark/bismark2report      |
      | rule       | fastqc             | bio/fastqc                      |
      | checkpoint | cairosvg           | utils/cairosvg                  |
      | checkpoint | bam2fq/interleaved | bio/samtools/bam2fq/interleaved |

  Scenario Outline: Complete wrapper name for non-standard version tag
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "<tag>/<short_name>"
    """
    And add snakemake facet with wrappers loaded
    When I put the caret after <short_name>
    And I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      wrapper: "<tag>/<full_name>"
    """
    Examples:
      | rule_like  | tag     | short_name         | full_name                       |
      | rule       | master  | bismark2report     | bio/bismark/bismark2report      |
      | rule       | latest  | fastqc             | bio/fastqc                      |
      | checkpoint | 0.54.3  | cairosvg           | utils/cairosvg                  |
      | checkpoint | 0.30.10 | bam2fq/interleaved | bio/samtools/bam2fq/interleaved |

  # TODO: configure temp wrappers dir using VFS & light fixture, e.g.
#  Scenario Outline: Complete for custom wrappers repo folder wrappers
#    Given a snakemake project
#    And with snakemake facet
#    And a file "wrappers_repo/bio/bismark/custom_wr1/wrapper.py" with text
#    """
#    """
#    And a file "wrappers_repo/bio/bismark/custom_wr1/meta.yam" with text
#    """
#    """
#    And a file "wrappers_repo/bio/bismark/custom_wr1/environment.yaml" with text
#    """
#    """
#    Given wrapper repo path "wrappers_repo"

  Scenario Outline: Complete for custom wrappers repo folder wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "<prefix>"
    """
    And add snakemake facet without wrappers loaded
    And wrapper repo path in test dir "wrappers_storage2"
    When I put the caret after <prefix>
    And I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      wrapper: "<tag>/bio/bismark/custom_wr1"
    """
    Examples:
      | rule_like  | tag     | prefix         |
      | rule       | master  | master/custom_wr1         |
      | rule       | latest  | latest/custom_wr1         |
      | checkpoint | 0.54.3  | 0.54.3/custom_wr1         |
      | checkpoint | 0.54.3  | 0.54.3/bismark            |
      | checkpoint | 0.30.10 | 0.30.10/bismark/custom |
      | checkpoint | file://${TEST_DATA}/wrappers_storage2 | file:// |
      | checkpoint | file://${TEST_DATA}/wrappers_storage2 | file://custom |
      | checkpoint | file://${TEST_DATA}/wrappers_storage2  | file://custom_wr1 |