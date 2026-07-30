package pt.ulusofona.ulht.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * University degree specific data for credential subject
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "University degree information")
public class UniversityDegreeData {
    
    @Schema(description = "Type of degree", example = "BachelorDegree")
    private String type;
    
    @Schema(description = "Name of the degree", example = "Bachelor of Science and Arts")
    private String name;
    
    @Schema(description = "Field of study", example = "Computer Science")
    private String field;
    
    @Schema(description = "Institution name", example = "ULHT - Universidade Lusófona")
    private String institution;
    
    @Schema(description = "Date degree was awarded")
    private String awardDate;
    
    @Schema(description = "GPA or grade")
    private String grade;
}

