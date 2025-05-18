package br.com.satyan.stering.saita.financasbottelegram.application.usecases;

import br.com.satyan.stering.saita.financasbottelegram.application.port.in.TelegramPortIn;
import br.com.satyan.stering.saita.financasbottelegram.application.port.out.S3PortOut;
import org.springframework.stereotype.Service;

@Service
public class ProcessPaymentMessageUsecase {

    private TelegramPortIn telegramPortIn;
    private S3PortOut s3PortOut;

    public ProcessPaymentMessageUsecase(TelegramPortIn telegramPortIn, S3PortOut s3PortOut) {
        this.telegramPortIn = telegramPortIn;
        this.s3PortOut = s3PortOut;
    }

    public void processPaymentMessage(String imageId) {
        byte[] imageBytes = telegramPortIn.getFile(imageId);
        var fileName = imageId + ".jpg";
        s3PortOut.uploadPhoto(fileName, imageBytes);
    }
}
