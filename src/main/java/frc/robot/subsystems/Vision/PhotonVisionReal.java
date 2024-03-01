package frc.robot.subsystems.Vision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.littletonrobotics.junction.AutoLogOutput;
import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose3d;
import frc.robot.Constants.VisionConstants;

public class PhotonVisionReal implements PhotonVisionIO {
    private final PhotonCamera left;
    private final PhotonCamera right;
    private final AprilTagFieldLayout aprilTagFieldLayout = AprilTagFields.k2024Crescendo.loadAprilTagLayoutField();
    private final PhotonPoseEstimator frontPoseEstimator;
    private final PhotonPoseEstimator rearPoseEstimator;
    private EstimatedRobotPose estimatedFrontPose = new EstimatedRobotPose(new Pose3d(), 0, new ArrayList<PhotonTrackedTarget>(), VisionConstants.POSE_STRATEGY);
    private EstimatedRobotPose estimatedRearPose = new EstimatedRobotPose(new Pose3d(), 0, new ArrayList<PhotonTrackedTarget>(), VisionConstants.POSE_STRATEGY);
    
    public PhotonVisionReal() {
        this.left = new PhotonCamera("photonvision_rear");
        this.right = new PhotonCamera("photonvision_front");
        this.frontPoseEstimator = new PhotonPoseEstimator(aprilTagFieldLayout, VisionConstants.POSE_STRATEGY, right, VisionConstants.ROBOT_TO_FRONT_CAMERA);
        this.rearPoseEstimator = new PhotonPoseEstimator(aprilTagFieldLayout, VisionConstants.POSE_STRATEGY, left, VisionConstants.ROBOT_TO_REAR_CAMERA);
    }

    public void updateInputs(PhotonVisionIOInputs inputs) {
        double frontAmbiguitySum = 0;
        double rearAmbiguitySum = 0;

        inputs.rearConfidence = 0;
        inputs.frontConfidence = 0;

        Optional<EstimatedRobotPose> frontPoseOptional = frontPoseEstimator.update();
        if (frontPoseOptional.isPresent()) {
            estimatedFrontPose = frontPoseOptional.get();

            inputs.estimatedFrontPose = estimatedFrontPose.estimatedPose;
            inputs.estimatedFrontPoseTimestamp = estimatedFrontPose.timestampSeconds;

            var frontTargetsSeen = estimatedFrontPose.targetsUsed.size();
            inputs.visibleFrontFiducialIDs = new int[frontTargetsSeen];

            for (int i = 0; i < frontTargetsSeen; i++) {
                var target = estimatedFrontPose.targetsUsed.get(i);
                inputs.visibleFrontFiducialIDs[i] = target.getFiducialId();
                frontAmbiguitySum += target.getPoseAmbiguity();
            }   

            inputs.frontConfidence = 1 - (frontAmbiguitySum / inputs.visibleFrontFiducialIDs.length);
        }
        Optional<EstimatedRobotPose> rearPoseOptional = rearPoseEstimator.update();
        
        if (rearPoseOptional.isPresent()) {
            estimatedRearPose = rearPoseOptional.get();

            inputs.estimatedRearPose = estimatedRearPose.estimatedPose;
            inputs.estimatedRearPoseTimestamp = estimatedRearPose.timestampSeconds;
        
            var rearTargetsSeen = estimatedRearPose.targetsUsed.size();
            inputs.visibleRearFiducialIDs = new int[rearTargetsSeen];
        
       
            for (int i = 0; i < rearTargetsSeen; i++) {
                var target = estimatedRearPose.targetsUsed.get(i);
                inputs.visibleFrontFiducialIDs[i] = target.getFiducialId();
                rearAmbiguitySum += target.getPoseAmbiguity();
            }  

            inputs.rearConfidence = 1 - (rearAmbiguitySum / inputs.visibleRearFiducialIDs.length); 
        } 
    }

    public List<PhotonTrackedTarget> getFrontTrackedTargets() {
        return estimatedFrontPose.targetsUsed;
    }

    public List<PhotonTrackedTarget> getRearTrackedTargets() {
        return estimatedRearPose.targetsUsed;
    }

    @AutoLogOutput
    public Pose3d[] getFrontTagPoses() {
        var frontTargets = estimatedFrontPose.targetsUsed;
        Pose3d[] frontTagPoses = new Pose3d[frontTargets.size()];
        for(int i = 0; i < frontTagPoses.length; i++) {
            frontTagPoses[i] = aprilTagFieldLayout.getTagPose(frontTargets.get(i).getFiducialId()).get();
        }
        return frontTagPoses;
    }

    @AutoLogOutput
    public Pose3d[] getRearTagPoses() {
        var rearTargets = estimatedRearPose.targetsUsed;
        Pose3d[] rearTagPoses = new Pose3d[rearTargets.size()];
        for(int i = 0; i < rearTagPoses.length; i++) {
            rearTagPoses[i] = aprilTagFieldLayout.getTagPose(rearTargets.get(i).getFiducialId()).get();
        }
        return rearTagPoses;
    }
}