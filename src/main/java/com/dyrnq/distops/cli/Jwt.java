package com.dyrnq.distops.cli;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(
        name = "jwt",
        aliases = {"j"},
        description = "create jwt secret")
public class Jwt extends CommonOptions implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        //        String jwtSecret = JwtUtils.createKey();
        //        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String jwtSecret = Encoders.BASE64.encode(Jwts.SIG.HS512.key().build().getEncoded());
        // System.out.println(jwtSecret);
        System.out.println("--jwt.secret=" + jwtSecret);
        System.out.println("JWT_SECRET=" + jwtSecret);
        return 0;
    }
}
