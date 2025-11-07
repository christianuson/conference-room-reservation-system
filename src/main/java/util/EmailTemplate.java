package util;

import model.Reservation;
import model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * HTML email templates for conference room system
 */
public class EmailTemplate {

    private static final String BRAND_COLOR = "#a51618";
    private static final String SUCCESS_COLOR = "#4CAF50";
    private static final String WARNING_COLOR = "#FF9800";
    private static final String DANGER_COLOR = "#f44336";

    /**
     * Base HTML template wrapper
     */
    private static String wrapTemplate(String title, String content, String accentColor) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f5f5f5;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f5f5f5; padding: 20px;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="background-color: %s; padding: 30px; text-align: center; border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: white; font-size: 24px;">Conference Room System</h1>
                                    </td>
                                </tr>
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        %s
                                    </td>
                                </tr>
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f9f9f9; padding: 20px 30px; text-align: center; border-radius: 0 0 8px 8px; border-top: 1px solid #e0e0e0;">
                                        <p style="margin: 0; color: #666; font-size: 12px;">
                                            This is an automated message from Conference Room Reservation System<br>
                                            Please do not reply to this email
                                        </p>
                                        <p style="margin: 10px 0 0 0; color: #999; font-size: 11px;">
                                            &copy; %d Conference Room System. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, title, accentColor, content, LocalDateTime.now().getYear());
    }

    /**
     * Reservation confirmation email (when user submits request)
     */
    public static String reservationConfirmation(User user, Reservation reservation) {
        String content = String.format("""
            <h2 style="color: #333; margin-top: 0;">Hello %s,</h2>
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                Thank you for submitting your reservation request. We have received your booking details 
                and it is now pending admin approval.
            </p>
            
            <div style="background-color: #fff3cd; border-left: 4px solid %s; padding: 15px; margin: 20px 0;">
                <h3 style="margin: 0 0 10px 0; color: #856404;">📋 Reservation Details</h3>
                <table style="width: 100%%; font-size: 14px;">
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Room:</td>
                        <td style="padding: 5px 0; color: #333;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Date:</td>
                        <td style="padding: 5px 0; color: #333;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Time:</td>
                        <td style="padding: 5px 0; color: #333;">%s - %s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Status:</td>
                        <td style="padding: 5px 0;">
                            <span style="background-color: %s; color: white; padding: 4px 12px; border-radius: 3px; font-size: 12px; font-weight: bold;">
                                PENDING APPROVAL
                            </span>
                        </td>
                    </tr>
                </table>
            </div>
            
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                An administrator will review your request shortly. You will receive another email once 
                your reservation has been approved or if any changes are needed.
            </p>
            
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                If you have any questions, please contact the facilities team.
            </p>
            
            <p style="color: #555; margin-top: 30px;">
                Best regards,<br>
                <strong>Conference Room Management Team</strong>
            </p>
            """,
                user.getUsername(),
                WARNING_COLOR,
                reservation.getRoomName(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                WARNING_COLOR
        );

        return wrapTemplate("Reservation Request Received", content, BRAND_COLOR);
    }

    /**
     * Reservation approval email
     */
    public static String reservationApproved(User user, Reservation reservation) {
        String content = String.format("""
            <h2 style="color: #333; margin-top: 0;">Great News, %s! 🎉</h2>
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                Your reservation request has been <strong style="color: %s;">APPROVED</strong>!
            </p>
            
            <div style="background-color: #e8f5e9; border-left: 4px solid %s; padding: 15px; margin: 20px 0;">
                <h3 style="margin: 0 0 10px 0; color: #2e7d32;">✅ Confirmed Reservation</h3>
                <table style="width: 100%%; font-size: 14px;">
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Room:</td>
                        <td style="padding: 5px 0; color: #333;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Date:</td>
                        <td style="padding: 5px 0; color: #333;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Time:</td>
                        <td style="padding: 5px 0; color: #333;">%s - %s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Status:</td>
                        <td style="padding: 5px 0;">
                            <span style="background-color: %s; color: white; padding: 4px 12px; border-radius: 3px; font-size: 12px; font-weight: bold;">
                                APPROVED
                            </span>
                        </td>
                    </tr>
                </table>
            </div>
            
            <div style="background-color: #e3f2fd; padding: 15px; margin: 20px 0; border-radius: 4px;">
                <p style="margin: 0; color: #1565c0; font-size: 13px;">
                    💡 <strong>Reminder:</strong> Please arrive on time and ensure the room is clean before you leave. 
                    If you need to cancel, please do so at least 24 hours in advance.
                </p>
            </div>
            
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                We look forward to hosting your meeting!
            </p>
            
            <p style="color: #555; margin-top: 30px;">
                Best regards,<br>
                <strong>Conference Room Management Team</strong>
            </p>
            """,
                user.getUsername(),
                SUCCESS_COLOR,
                SUCCESS_COLOR,
                reservation.getRoomName(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                SUCCESS_COLOR
        );

        return wrapTemplate("Reservation Approved", content, SUCCESS_COLOR);
    }

    /**
     * Reservation rejection email
     */
    public static String reservationRejected(User user, Reservation reservation) {
        String content = String.format("""
            <h2 style="color: #333; margin-top: 0;">Hello %s,</h2>
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                We regret to inform you that your reservation request could not be approved at this time.
            </p>
            
            <div style="background-color: #ffebee; border-left: 4px solid %s; padding: 15px; margin: 20px 0;">
                <h3 style="margin: 0 0 10px 0; color: #c62828;">❌ Reservation Not Approved</h3>
                <table style="width: 100%%; font-size: 14px;">
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Room:</td>
                        <td style="padding: 5px 0; color: #333;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Date:</td>
                        <td style="padding: 5px 0; color: #333;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Time:</td>
                        <td style="padding: 5px 0; color: #333;">%s - %s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Status:</td>
                        <td style="padding: 5px 0;">
                            <span style="background-color: %s; color: white; padding: 4px 12px; border-radius: 3px; font-size: 12px; font-weight: bold;">
                                REJECTED
                            </span>
                        </td>
                    </tr>
                </table>
            </div>
            
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                This may be due to a scheduling conflict or other administrative reasons. 
                Please feel free to submit a new reservation request for a different time slot.
            </p>
            
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                If you have questions about this decision, please contact the facilities team.
            </p>
            
            <p style="color: #555; margin-top: 30px;">
                Best regards,<br>
                <strong>Conference Room Management Team</strong>
            </p>
            """,
                user.getUsername(),
                DANGER_COLOR,
                reservation.getRoomName(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                DANGER_COLOR
        );

        return wrapTemplate("Reservation Not Approved", content, DANGER_COLOR);
    }

    /**
     * Reservation cancellation email
     */
    public static String reservationCancelled(User user, Reservation reservation) {
        String content = String.format("""
            <h2 style="color: #333; margin-top: 0;">Hello %s,</h2>
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                Your reservation has been successfully cancelled.
            </p>
            
            <div style="background-color: #fafafa; border-left: 4px solid #757575; padding: 15px; margin: 20px 0;">
                <h3 style="margin: 0 0 10px 0; color: #424242;">🗑️ Cancelled Reservation</h3>
                <table style="width: 100%%; font-size: 14px;">
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Room:</td>
                        <td style="padding: 5px 0; color: #333;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Date:</td>
                        <td style="padding: 5px 0; color: #333;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Time:</td>
                        <td style="padding: 5px 0; color: #333;">%s - %s</td>
                    </tr>
                    <tr>
                        <td style="padding: 5px 0; color: #666; font-weight: bold;">Status:</td>
                        <td style="padding: 5px 0;">
                            <span style="background-color: #757575; color: white; padding: 4px 12px; border-radius: 3px; font-size: 12px; font-weight: bold;">
                                CANCELLED
                            </span>
                        </td>
                    </tr>
                </table>
            </div>
            
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                The room is now available for other users to book. You're welcome to make a new 
                reservation anytime through the system.
            </p>
            
            <p style="color: #555; margin-top: 30px;">
                Best regards,<br>
                <strong>Conference Room Management Team</strong>
            </p>
            """,
                user.getUsername(),
                reservation.getRoomName(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime()
        );

        return wrapTemplate("Reservation Cancelled", content, BRAND_COLOR);
    }

    /**
     * Welcome email for new users
     */
    public static String welcomeEmail(User user) {
        String content = String.format("""
            <h2 style="color: #333; margin-top: 0;">Welcome to Conference Room System, %s! 👋</h2>
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                Thank you for registering with our Conference Room Reservation System. 
                Your account has been successfully created!
            </p>
            
            <div style="background-color: #e3f2fd; padding: 20px; margin: 20px 0; border-radius: 4px;">
                <h3 style="margin: 0 0 15px 0; color: #1565c0;">📚 Getting Started</h3>
                <ul style="margin: 0; padding-left: 20px; color: #555; line-height: 1.8;">
                    <li>Browse available conference rooms in the dashboard</li>
                    <li>Double-click any room card to make a reservation</li>
                    <li>Select your preferred date and time</li>
                    <li>Wait for admin approval (you'll receive an email notification)</li>
                    <li>Check "My Reservations" to view and manage your bookings</li>
                </ul>
            </div>
            
            <div style="background-color: #fff3cd; padding: 15px; margin: 20px 0; border-radius: 4px;">
                <p style="margin: 0; color: #856404; font-size: 13px;">
                    💡 <strong>Tip:</strong> Book rooms in advance to ensure availability. 
                    All reservations require admin approval for quality control.
                </p>
            </div>
            
            <p style="color: #555; line-height: 1.6; font-size: 14px;">
                If you have any questions or need assistance, please don't hesitate to contact our support team.
            </p>
            
            <p style="color: #555; margin-top: 30px;">
                Best regards,<br>
                <strong>Conference Room Management Team</strong>
            </p>
            """,
                user.getUsername()
        );

        return wrapTemplate("Welcome to Conference Room System", content, SUCCESS_COLOR);
    }
}