/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.exception;

/**
 *
 * @author danil
 */
public class InvalidPageRequestException extends RuntimeException {
    private final String errorCode = "INVALID_PAGE_REQUEST";
    private final int page;
    private final int size;

    public InvalidPageRequestException(int page, int size) {
        super(String.format("Invalid pagination parameters: page=%d, size=%d", page, size));
        this.page = page;
        this.size = size;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}

