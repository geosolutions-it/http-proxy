package it.geosolutions.httpproxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

public class StubServletOutputStream extends ServletOutputStream {
 public ByteArrayOutputStream baos = new ByteArrayOutputStream();
   public void write(int i) throws IOException {
    baos.write(i);
 }
}