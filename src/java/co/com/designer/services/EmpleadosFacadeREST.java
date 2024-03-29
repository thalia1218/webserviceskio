package co.com.designer.services;

import co.com.designer.kiosko.generales.EnvioCorreo;
import co.com.designer.kiosko.entidades.ConexionesKioskos;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author thalia
 */
@Stateless
@Path("empleados")
public class EmpleadosFacadeREST {
    
    protected EntityManager getEntityManager() {
        String unidadPersistencia="wsreportePU";
        EntityManager em = Persistence.createEntityManagerFactory(unidadPersistencia).createEntityManager();
        return em;
    }
    
    protected EntityManager getEntityManager(String persistence) {
        String unidadPersistencia=persistence;
        EntityManager em = Persistence.createEntityManagerFactory(unidadPersistencia).createEntityManager();
        return em;
    }
    
    protected void setearPerfil() {
        try {
            System.out.println("setearPerfil()");
            String rol = "ROLKIOSKO";
            String sqlQuery = "SET ROLE " + rol + " IDENTIFIED BY RLKSK ";
            Query query = getEntityManager().createNativeQuery(sqlQuery);
            query.executeUpdate();
        } catch (Exception ex) {
            System.out.println("Error setearPerfil: " + ex);
        }
    }
    
    protected void setearPerfil(String esquema, String cadenaPersistencia) {
        try {
            String rol = "ROLKIOSKO";
            if (esquema != null && !esquema.isEmpty()) {
                rol = rol + esquema.toUpperCase();
            }
            System.out.println("setearPerfil(esquema, cadena)");
            String sqlQuery = "SET ROLE " + rol + " IDENTIFIED BY RLKSK ";
            Query query = getEntityManager(cadenaPersistencia).createNativeQuery(sqlQuery);
            query.executeUpdate();
        } catch (Exception ex) {
            System.out.println("Error setearPerfil(cadenaPersistencia): " + ex);
        }
    }     
    
    @GET
    @Path("/datosEmpleadoNit/{empleado}/{nit}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getDatosEmpleadoNit(@PathParam("empleado") String empleado, @PathParam("nit") String nitEmpresa, @QueryParam("cadena") String cadena) {
        System.out.println("parametros getDatosEmpleadosNit():  empleado: " + empleado + " nit: " + nitEmpresa + " cadena " + cadena);
        List s = null;
        try {
            String documento = getDocumentoPorSeudonimo(empleado, nitEmpresa, cadena);
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = " select  \n"
                    + "e.codigoempleado usuario,   \n"
                    + "p.nombre ||' '|| p.primerapellido ||' '|| p.segundoapellido nombres,  \n"
                    + "p.primerapellido apellido1,  \n"
                    + "p.segundoapellido apellido2,   \n"
                    + "decode(p.sexo,'M', 'MASCULINO', 'F', 'FEMENINO', '') sexo,   \n"
                    + "to_char(p.FECHANACIMIENTO, 'dd-MM-yyyy') fechaNacimiento,  \n"
                    + "(select nombre from ciudades where secuencia=p.CIUDADNACIMIENTO) ciudadNacimiento,  \n"
                    + "p.GRUPOSANGUINEO grupoSanguineo,  \n"
                    + "p.FACTORRH factorRH,  \n"
                    + "(select nombrelargo from tiposdocumentos where secuencia=p.TIPODOCUMENTO) tipoDocu,  \n"
                    + "p.NUMERODOCUMENTO documento,   \n"
                    + "(select nombre from ciudades where secuencia=p.CIUDADDOCUMENTO) lugarExpediDocu,  \n"
                    + "p.EMAIL email,  \n"
                    + "'DIRECCION' direccion,   \n"
                    + "ck.ULTIMACONEXION ultimaConexion,\n"
                    + "em.codigo codigoEmpresa,  \n"
                    + "em.nit nitEmpresa,   \n"
                    + "em.nombre nombreEmpresa,  \n"
                    + "empleadocurrent_pkg.descripciontipocontrato(e.secuencia, sysdate) contrato,  \n"
                    + "trim(to_char(empleadocurrent_pkg.ValorBasicoCorte(e.secuencia, sysdate),'$999G999G999G999G999G999')) salario,    \n"
                    + "empleadocurrent_pkg.DescripcionCargoCorte(e.secuencia, sysdate) cargo,  \n"
                    //+ "         DIAS360(empleadocurrent_pkg.FechaVigenciaTipoContrato(e.secuencia, sysdate), sysdate) diasW,    +\n"
                    + "empleadocurrent_pkg.FechaVigenciaTipoContrato(e.secuencia, sysdate) inicioContratoActual,\n"
                    //+ "         (select nombre from estructuras where secuencia=empleadocurrent_pkg.estructuracargocorte(e.secuencia, sysdate)) estructura,   +\n"
                    + "em.logo logoEmpresa,  \n"
                    + "UPPER(empleadocurrent_pkg.DireccionAlternativa(p.secuencia, sysdate)) direccionPersona,   \n"
                    + "empleadocurrent_pkg.CentrocostoNombre(e.secuencia) centroscostos,   \n"
                    + "empleadocurrent_pkg.EdadPersona(p.secuencia, sysdate) || ' AÑOS' edad,   \n"
                    + "empleadocurrent_pkg.entidadafp(e.secuencia) entidadfp,\n"
                    + "empleadocurrent_pkg.entidadeps(e.secuencia) entidadeps,  \n"
                    + "empleadocurrent_pkg.entidadarp(e.secuencia)  entidadarp,\n"
                    + "empleadocurrent_pkg.entidadcesantias(e.secuencia, sysdate) entidadcesantias,\n"
                   // + "empleadocurrent_pkg.Afiliacion(e.secuencia , te.codigo, sysdate, sysdate) cajaCompensacion,           \n"
                    + "empleadocurrent_pkg.Afiliacion(e.secuencia , 14, sysdate, sysdate) cajaCompensacion,           \n"
                    + "to_char(empleadocurrent_pkg.fechaVigenciaTipoContrato(e.secuencia, sysdate), 'dd-MM-yyyy') fechaContratacion    \n"
                    + "from   \n"
                    + "empleados e, conexioneskioskos ck, empresas em, personas p "
                   // + "VigenciasAfiliaciones V , tiposentidades te  \n"
                    + "where  \n"
                    + "e.persona=p.secuencia   \n"
                    + "and e.empresa=em.secuencia    \n"
                    + "and ck.empleado=e.secuencia \n"
                  //  + "and e.secuencia(+) = v.empleado \n"
                  //  + "and te.secuencia= v.tipoentidad (+)\n"
                  //  + "and te.codigo = 14 \n"
                    + "and p.numerodocumento= ?  \n"
                    + "and em.nit=? ";
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, documento);
            query.setParameter(2, nitEmpresa);

            s = query.getResultList();
            s.forEach(System.out::println);
            return Response.status(Response.Status.OK).entity(s).build();
        } catch (Exception ex) {
            System.out.println("Error "+this.getClass().getName()+"getDatosEmpleadoNit: " + ex);
            return Response.status(Response.Status.NOT_FOUND).entity("Error").build();
        }
    }
    
    @GET
    @Path("/datosFamiliaEmpleado/{empleado}/{nit}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getDatosFamiliaEmpleado(@PathParam("empleado") String empleado, @PathParam("nit") String nitEmpresa, @QueryParam("cadena") String cadena) {
        System.out.println("parametros getDatosFamiliaEmpleado(): empleado: " + empleado + " nit: " + nitEmpresa + " cadena: " + cadena);
        List s = null;
        try {
            String secEmpl = getSecuenciaEmplPorSeudonimo(empleado, nitEmpresa, cadena);
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "select \n"
                    + "fam.nombre ||' '|| fam.primerapellido ||' '|| fam.segundoapellido nombreFamiliar, t.tipo Parentesco,\n"
                    + "decode(ltel.secuencia, null, ' ',\n"
                    + "ltel.tipotelefono||' - '||\n"
                    + "--to_char(ltel.fechavigencia,'DD/MM/YYYY')||' '||\n"
                    + "ltel.numerotelefono) telefono\n"
                    + "from empleados e, empresas em, personas p, familiares f, tiposfamiliares t, personas fam , \n"
                    + "(select tel.secuencia, tel.persona, tel.fechavigencia, tel.numerotelefono, ttel.nombre tipotelefono\n"
                    + "from telefonos tel, tipostelefonos ttel\n"
                    + "where ttel.secuencia = tel.tipotelefono\n"
                    + "and tel.fechavigencia = (select max(teli.fechavigencia) \n"
                    + "    from telefonos teli\n"
                    + "    where teli.persona = tel.persona\n"
                    + "    and teli.tipotelefono = tel.tipotelefono) ) ltel\n"
                    + "where   \n"
                    + "p.secuencia = e.persona    \n"
                    + "and e.empresa = em.secuencia    \n"
                    + "and f.persona = p.secuencia      \n"
                    + "and f.personafamiliar=fam.secuencia  \n"
                    + "and t.secuencia = f.tipofamiliar \n"
                    + "and fam.secuencia = ltel.persona(+) \n"
                    + "and e.secuencia = ? "
                    + "order by fam.nombre, fam.primerapellido, fam.segundoapellido, ltel.tipotelefono";
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, secEmpl);

            s = query.getResultList();
            s.forEach(System.out::println);
            return Response.status(Response.Status.OK).entity(s).build();
        } catch (Exception ex) {
            System.out.println("Error "+this.getClass().getName()+".getDatosFamiliaEmpleado: " + ex);
            return Response.status(Response.Status.NOT_FOUND).entity("Error").build();
        }
    }
    
    
    @GET
    @Path("/telefonosEmpleadoNit")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getTelefonosEmpleado(@QueryParam("usuario") String seudonimo, @QueryParam("nit") String nitEmpresa, @QueryParam("cadena") String cadena) {
        System.out.println("parametros getTelefonosEmpleado(): empleado: "+seudonimo+" nit: "+nitEmpresa+ " cadena "+cadena);
        List s = null;
        try {
        String documento = getDocumentoPorSeudonimo(seudonimo, nitEmpresa, cadena);
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "select "
                    + "t.numerotelefono numeroTelefono,  "
                    + "tt.nombre tipoTelefono "
                    + "from empleados e, conexioneskioskos ck, empresas em, personas p, telefonos t, tipostelefonos tt "
                    + "where ck.empleado=e.secuencia "
                    + "and e.persona=p.secuencia "
                    + "and e.empresa=em.secuencia   "
                    + "and e.persona = t.persona(+) "
                    + "and t.fechavigencia = (select max(ti.fechavigencia) "
                    + "                        from telefonos ti where ti.persona = t.persona and ti.fechavigencia <= sysdate "
                    + ") "
                    + "and t.tipotelefono = tt.secuencia(+) "
                    + "and p.numerodocumento= ? "
                    + "and em.nit= ? ";
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, documento);
            query.setParameter(2, nitEmpresa);

            s = query.getResultList();
            s.forEach(System.out::println);
            return Response.status(Response.Status.OK).entity(s).build();
        } catch (Exception ex) {
            System.out.println("Error "+this.getClass().getName()+".getTelefonosEmpleado(): " + ex);
            return Response.status(Response.Status.NOT_FOUND).entity("Error").build();
        }
    }    
    
    
    /**
     * Devuelve Lista de experiencias laborales
     * @param secuenciaEmpleado, nitEmpresa, cadena
     * @return Lista de experiencias laborales
     */    
    @GET
    @Path("/datosExperienciaLab")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getDatosExperienciaLab(@QueryParam("empleado") String empleado, @QueryParam("nit") String nitEmpresa, @QueryParam("cadena") String cadena) {
        System.out.println("parametros getDatosExperienciaLab(): empleado: " + empleado + " nit: " + nitEmpresa + " cadena: " + cadena);
        List exLab = null;
        try {
            String secEmpl = getDocumentoPorSeudonimo(empleado, nitEmpresa, cadena);
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "select \n" +
                              "p.numerodocumento,\n" +
                              "ex.empresa empresa,\n" +
                              "to_char(ex.fechadesde,'DD/MM/YYYY'),\n" +
                              "to_char(ex.fechahasta,'DD/MM/YYYY'),\n" +
                              "ex.jefeinmediato,\n" +
                              "ex.telefono,\n" +
                              "ex.cargo,\n" +
                              "ex.alcance,\n" +
                              "mo.nombre motivo,\n" +
                              "sec.descripcion sectorec\n" +
                              "from hvexperienciaslaborales ex,hvhojasdevida hv, personas p, motivosretiros mo, \n" +
                              "sectoreseconomicos sec, empleados e \n" +
                              "where ex.hojadevida = hv.secuencia \n" +
                              "and e.codigoempleado = p.numerodocumento \n" +
                              "and hv.persona = p.secuencia \n" +
                              "and ex.motivoretiro = mo.secuencia(+) \n" +
                              "and ex.sectoreconomico = sec.secuencia(+) \n" +
                              "and p.numerodocumento = ? ";
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, secEmpl);

            exLab = query.getResultList();
            exLab.forEach(System.out::println);
            return Response.status(Response.Status.OK).entity(exLab).build();
        } catch (Exception ex) {
            System.out.println("Error "+this.getClass().getName()+".getDatosExperienciaLab: " + ex);
            return Response.status(Response.Status.NOT_FOUND).entity("Error").build();
        }
    }    
    
    public String getDocumentoCorreoODocumento(String usuario, String nitEmpresa, String cadena) {
       System.out.println("Parametros getDocumentoCorreoODocumento() usuario: "+usuario+", cadena: "+cadena);
       String documento=null;
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "SELECT P.NUMERODOCUMENTO DOCUMENTO FROM PERSONAS P WHERE lower(P.EMAIL)=lower(?)";
            if (this.validarCodigoUsuario(usuario)) {
                 sqlQuery+=" OR P.NUMERODOCUMENTO=?"; // si el valor es numerico validar por numero de documento
            }
            System.out.println("Query: "+sqlQuery);
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);

            query.setParameter(1, usuario);
            if (this.validarCodigoUsuario(usuario)) {
               query.setParameter(2, usuario);
            }
            documento =  query.getSingleResult().toString();
        } catch (Exception e) {
            System.out.println("Error: "+ConexionesKioskosFacadeREST.class.getName()+" getDocumentoCorreoODocumento: "+e.getMessage());
            try {
                String sqlQuery2 = "SELECT P.NUMERODOCUMENTO DOCUMENTO "
                        + "FROM PERSONAS P, EMPLEADOS E "
                        + "WHERE "
                        + "P.SECUENCIA=E.PERSONA "
                        + "AND (lower(P.EMAIL)=lower(?)";
                if (this.validarCodigoUsuario(usuario)) {
                    sqlQuery2 += " OR E.CODIGOEMPLEADO=?"; // si el valor es numerico validar por codigoempleado
                }
                sqlQuery2+=")";
                System.out.println("Query2: " + sqlQuery2);
                Query query2 = getEntityManager(cadena).createNativeQuery(sqlQuery2);
                query2.setParameter(1, usuario);
                if (this.validarCodigoUsuario(usuario)) {
                    query2.setParameter(2, usuario);
                }
                documento =  query2.getSingleResult().toString();
                System.out.println("Validación documentoPorEmpleado: "+documento);
            } catch (Exception ex) {
                System.out.println("Error 2: " + ConexionesKioskos.class.getName() + " getDocumentoCorreoODocumento(): ");
            }
        }
        return documento;
   }
    
    public String getDocumentoPorSeudonimo(String seudonimo, String nitEmpresa, String cadena) {
       System.out.println("Parametros getDocumentoPorSeudonimo() seudonimo: "+seudonimo+", nitEmpresa: "+nitEmpresa+", cadena: "+cadena);
       String documento=null;
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "SELECT P.NUMERODOCUMENTO DOCUMENTO FROM PERSONAS P, CONEXIONESKIOSKOS CK WHERE CK.PERSONA=P.SECUENCIA AND lower(CK.SEUDONIMO)=lower(?) AND CK.NITEMPRESA=?";
            System.out.println("Query: "+sqlQuery);
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);

            query.setParameter(1, seudonimo);
            query.setParameter(2, nitEmpresa);
            documento =  query.getSingleResult().toString();
            System.out.println("documento: "+documento);
        } catch (Exception e) {
            System.out.println("Error: "+this.getClass().getName()+".getDocumentoPorSeudonimo: "+e.getMessage());
        }
        return documento;
   } 
      
    public String getSecuenciaEmplPorSeudonimo(String seudonimo, String nitEmpresa, String cadena) {
        System.out.println("Parametros getSecuenciaEmplPorSeudonimo(): seudonimo: "+seudonimo+", nitEmpresa: "+nitEmpresa+", cadena: "+cadena);
       String secuencia=null;
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "SELECT E.SECUENCIA SECUENCIAEMPLEADO FROM EMPLEADOS E, CONEXIONESKIOSKOS CK WHERE CK.EMPLEADO=E.SECUENCIA AND CK.SEUDONIMO=? AND CK.NITEMPRESA=?";
            System.out.println("Query: "+sqlQuery);
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);

            query.setParameter(1, seudonimo);
            query.setParameter(2, nitEmpresa);
            secuencia =  query.getSingleResult().toString();
            System.out.println("secuencia: "+secuencia);
        } catch (Exception e) {
            System.out.println("Error: "+this.getClass().getName()+".getSecuenciaEmplPorSeudonimo: "+e.getMessage());
        }
        return secuencia;
   }    
    
    public String getSecuenciaPorNitEmpresa( String nitEmpresa, String cadena) {
       String secuencia=null;
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "SELECT EM.SECUENCIA SECUENCIAEMPRESA FROM EMPRESAS EM WHERE EM.NIT=?";
            System.out.println("Query: "+sqlQuery);
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, nitEmpresa);
            secuencia =  query.getSingleResult().toString();
            System.out.println("secuencia: "+secuencia);
        } catch (Exception e) {
            System.out.println("Error: "+this.getClass().getName()+".getSecuenciaPorNitEmpresa: "+e.getMessage());
        }
        return secuencia;
   }  
    
    
    
    public boolean validarCodigoUsuario(String usuario) {
        boolean resultado = false;
        BigInteger numUsuario;
        try {
            numUsuario = new BigInteger(usuario);
            resultado = true;
        } catch (NumberFormatException nfe) {
            resultado = false;
        }
        return resultado;
    }
    
    @GET
    @Path("{usuario}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecuencia(@PathParam("usuario") String usuario, @QueryParam("cadena") String cadena) {
        System.out.println("Parametros getSecuencia(): usuario: "+usuario+", cadena: "+cadena);
        BigDecimal res = null;
        try {
            //String esquema = getEsquema(nitEmpresa, cadena);
            //setearPerfil(esquema, cadena);
            String sqlQuery = "SELECT EMPLEADO FROM CONEXIONESKIOSKOS WHERE SEUDONIMO=?";
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, usuario);
            res = (BigDecimal) query.getSingleResult();
        } catch (Exception ex) {
            System.out.println("Error "+this.getClass().getName() + ex);
            res = BigDecimal.ZERO;
        }
        return res.toString();
    }
    
   public BigDecimal consultarCodigoJornada(String seudonimo, String nitEmpresa, String fechaDisfrute, String cadena) throws Exception {
       System.out.println("Parametros consultarCodigoJornada()): seudonimo: "+seudonimo+", nitEmpresa: "+nitEmpresa+", fechaDisfrute: "+fechaDisfrute+", cadena: "+cadena);
        String consulta = "select nvl(j.codigo, 1) "
                + "from vigenciasjornadas v, jornadaslaborales j "
                + "where v.empleado = ? "
                + "and j.secuencia = v.jornadatrabajo "
                + "and v.fechavigencia = (select max(vi.fechavigencia) "
                + "from vigenciasjornadas vi "
                + "where vi.empleado = v.empleado "
                + "and vi.fechavigencia <= to_date( ? , 'dd/mm/yyyy') ) ";
        Query query = null;
        BigDecimal codigoJornada;
        String secEmpleado=getSecuenciaEmplPorSeudonimo(seudonimo, nitEmpresa, cadena);
        /*SimpleDateFormat formatoFecha = new SimpleDateFormat("ddMMyyyy");
        String strFechaDisfrute = formatoFecha.format(fechaDisfrute);*/
        System.out.println("secuencia: " + secEmpleado);
        //System.out.println("fecha en txt: " + strFechaDisfrute);
        System.out.println("fecha en txt: " + fechaDisfrute);
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            query = getEntityManager(cadena).createNativeQuery(consulta);
            query.setParameter(1, secEmpleado);
            //query.setParameter(2, strFechaDisfrute);
            query.setParameter(2, fechaDisfrute);
            codigoJornada = new BigDecimal(query.getSingleResult().toString());
            return codigoJornada;
        } catch (PersistenceException pe) {
            System.out.println("Error de persistencia.");
            throw new Exception(pe.toString());
        } catch (NullPointerException npee) {
            System.out.println("Nulo general");
//            throw new Exception(npee.toString());
            return null;
        } catch (Exception e) {
            System.out.println("Error general. " + e);
            throw new Exception(e.toString());
        }
    }    
   
   
    private boolean validaFechaPago(String seudonimo, String nitEmpresa, String fechainicialdisfrute, String cadena) throws Exception {
            Calendar cl = Calendar.getInstance();
            cl.setTime(getFechaUltimoPago(seudonimo, nitEmpresa, cadena));
            return getDate(fechainicialdisfrute, cadena).after(cl.getTime());
    }   
   
    public Date getFechaUltimoPago(String seudonimo, String nitEmpresa, String cadena) throws Exception {
        System.out.println("Parametros getFechaUltimoPago(): seudonimo: "+seudonimo+", nitempresa: "+nitEmpresa+", cadena: "+cadena);
        BigDecimal res = null;
        try {
        String esquema = getEsquema(nitEmpresa, cadena);
        setearPerfil(esquema, cadena);
        String secEmpleado=getSecuenciaEmplPorSeudonimo(seudonimo, nitEmpresa, cadena);
        String consulta = "SELECT GREATEST("
                + "CORTESPROCESOS_PKG.CAPTURARCORTEPROCESO(?, 1), "
                + "NVL( CORTESPROCESOS_PKG.CAPTURARCORTEPROCESO(?, 80), CORTESPROCESOS_PKG.CAPTURARCORTEPROCESO(?, 1)"
                + ")) "
                + "FROM DUAL ";
        Date fechaUltimoPago = null;
            Query query = getEntityManager(cadena).createNativeQuery(consulta);
            query.setParameter(1, secEmpleado);
            query.setParameter(2, secEmpleado);
            query.setParameter(3, secEmpleado);
            fechaUltimoPago = (Date) (query.getSingleResult());
            return fechaUltimoPago;
        } catch (PersistenceException pe) {
            System.out.println("Error de persistencia.");
            throw new Exception(pe.toString());
        } catch (NullPointerException npee) {
            System.out.println("Nulo general");
            throw new Exception(npee.toString());
        } catch (Exception e) {
            System.out.println("Error "+this.getClass().getName()+".getFechaUltimoPago():  " + e);
            throw new Exception(e.toString());
        }
    }
    
    private String nombreDia(int dia) {
        String retorno = "";
        switch (dia) {
            case 1:
                retorno = "DOM";
                break;
            case 2:
                retorno = "LUN";
                break;
            case 3:
                retorno = "MAR";
                break;
            case 4:
                retorno = "MIE";
                break;
            case 5:
                retorno = "JUE";
                break;
            case 6:
                retorno = "VIE";
                break;
            case 7:
                retorno = "SAB";
                break;
            default:
                retorno = "";
                break;
        }
        return retorno;
    }
    
    public Date getDate(String fechaStr, String cadena) throws PersistenceException, NullPointerException, Exception {
        System.out.println(this.getClass().getName() + "." + "getDate" + "()");
        String consulta = "SELECT "
                + "TO_DATE(?, 'dd/mm/yyyy') "
                + "FROM DUAL ";
        Query query = null;
        Date fechaRegreso = null;
        try {
            query = getEntityManager(cadena).createNativeQuery(consulta);
            query.setParameter(1, fechaStr);
            fechaRegreso = (Date) (query.getSingleResult());
            return fechaRegreso;
        } catch (PersistenceException pe) {
            System.out.println("Error de persistencia en calculaFechaRegreso.");
            throw new Exception(pe.toString());
        } catch (NullPointerException npee) {
            System.out.println("Nulo general en "+this.getClass().getName()+".calculaFechaRegreso");
            throw new Exception(npee.toString());
        } catch (Exception e) {
            System.out.println("Error general en calculaFechaRegreso. " + e);
            throw new Exception(e.toString());
        }
    }    
       
    public boolean verificarDiaLaboral(String fechaDisfrute, BigDecimal codigoJornada, String nitEmpresa, String cadena) throws Exception {
        System.out.println(this.getClass().getName() + "." + "verificarDiaLaboral(): fechaDisfrute: "+fechaDisfrute+", codigoJornada: "+codigoJornada+", cadena: "+cadena);
        System.out.println("fechaDisfrute: " + fechaDisfrute);
        System.out.println("codigoJornada: " + codigoJornada);
        String consulta = "select COUNT(*) "
                + "FROM JORNADASSEMANALES JS, JORNADASLABORALES JL "
                + "WHERE JL.SECUENCIA = JS.JORNADALABORAL "
                + "AND JL.CODIGO = TO_number( ? ) "
                + "AND JS.DIA = ? ";
        Query query = null;
        BigDecimal conteoDiaLaboral;
        boolean esDiaLaboral;
        int diaSemana;
        String strFechaDisfrute = "";
        GregorianCalendar c = new GregorianCalendar();
        String esquema = getEsquema(nitEmpresa, cadena);
        setearPerfil(esquema, cadena);
        c.setTime(getDate(fechaDisfrute, cadena));
        diaSemana = c.get(Calendar.DAY_OF_WEEK);
        strFechaDisfrute = nombreDia(diaSemana);
        System.out.println("strFechaDisfrute: " + strFechaDisfrute);
        try {
            query = getEntityManager(cadena).createNativeQuery(consulta);
            query.setParameter(1, codigoJornada);
            query.setParameter(2, strFechaDisfrute);
            conteoDiaLaboral = new BigDecimal(query.getSingleResult().toString());
            esDiaLaboral = !conteoDiaLaboral.equals(BigDecimal.ZERO);
            return esDiaLaboral;
        } catch (PersistenceException pe) {
            System.out.println("Error de persistencia.");
            throw new Exception(pe.toString());
        } catch (NullPointerException npee) {
            System.out.println("Nulo general");
            throw new Exception(npee.toString());
        } catch (Exception e) {
            System.out.println("Error "+this.getClass().getName()+".verificarDiaLaboral(): " + e);
            throw new Exception(e.toString());
        }
    }
    
    public boolean verificarFestivo(String fechaDisfrute, String nitEmpresa, String cadena) throws Exception {
        System.out.println(this.getClass().getName() + "." + "verificarFestivo(): fechaDisfrute: "+fechaDisfrute+", cadena: "+cadena);
        String consulta = "select COUNT(*) "
                + "FROM FESTIVOS F, PAISES P "
                + "WHERE P.SECUENCIA = F.PAIS "
                + "AND P.NOMBRE = ? "
                + "AND F.DIA = TO_DATE( ? , 'DD/MM/YYYY') ";
        Query query = null;
        BigDecimal conteoDiaFestivo;
        boolean esDiaFestivo;
        /*SimpleDateFormat formatoFecha = new SimpleDateFormat("ddMMyyyy");
        String strFechaDisfrute = formatoFecha.format(fechaDisfrute);*/
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            query = getEntityManager(cadena).createNativeQuery(consulta);
            query.setParameter(1, "COLOMBIA");
            //query.setParameter(2, strFechaDisfrute);
            query.setParameter(2, fechaDisfrute);
            conteoDiaFestivo = new BigDecimal(query.getSingleResult().toString());
            esDiaFestivo = !conteoDiaFestivo.equals(BigDecimal.ZERO);
            return esDiaFestivo;
        } catch (PersistenceException pe) {
            System.out.println("Error de persistencia.");
            throw new Exception(pe.toString());
        } catch (NullPointerException npee) {
            System.out.println("Nulo general");
            throw new Exception(npee.toString());
        } catch (Exception e) {
            System.out.println("Error verificarFestivo(): fechaDisfrute: "+fechaDisfrute+", cadena: "+cadena + e);
            throw new Exception(e.toString());
        }
//        return false;
    }
      

    public BigDecimal consultaTraslapamientos(
             String seudonimo,
             String nitEmpresa, 
             String fechaIniVaca, 
             String fechaFinVaca,
             String cadena) throws PersistenceException, NullPointerException, Exception {
        System.out.println(this.getClass().getName() + "." + "consultaTraslapamientos" + "()");
        String secEmpleado = getSecuenciaEmplPorSeudonimo(seudonimo, nitEmpresa, cadena);
        String consulta = "SELECT "
                + "KIOVACACIONES_PKG.VERIFICARTRASLAPAMIENTO(?, ? , ? ) "
                + "FROM DUAL ";
        Query query = null;
        BigDecimal contTras = null;
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            query = getEntityManager(cadena).createNativeQuery(consulta);
            query.setParameter(1, secEmpleado);
            //query.setParameter(2, fechaIniVaca, TemporalType.DATE);
            //query.setParameter(3, fechaFinVaca, TemporalType.DATE);
            query.setParameter(2, fechaFinVaca);
            query.setParameter(3, fechaFinVaca);
            contTras = (BigDecimal) (query.getSingleResult());
            System.out.println("Resultado consulta traslapamiento: " + contTras);
            return contTras;
        } catch (PersistenceException pe) {
            System.out.println("Error de persistencia en consultaTraslapamientos.");
            throw new Exception(pe.toString());
        } catch (NullPointerException npee) {
            System.out.println("Nulo general en consultaTraslapamientos");
            throw new Exception(npee.toString());
        } catch (Exception e) {
            System.out.println("Error general en "+this.getClass().getName()+"consultaTraslapamientos(). " + e);
            throw new Exception(e.toString());
        }
    }
    
    
    /*
       Método que valida si la fecha de disfrute que recibe como parametro ya tiene una solicitud asociada
    */
    public BigDecimal verificaExistenciaSolicitud(
            String seudonimo, 
            String nitEmpresa,
            String fechaIniVaca,
            String cadena) throws Exception {
        System.out.println("Parametros verificaExistenciaSolicitud(): seudonimo: "+seudonimo+", nitempresa: "+nitEmpresa+", fechaIniVaca: "+fechaIniVaca+", cadena: "+cadena);
        String secEmpleado = getSecuenciaEmplPorSeudonimo(seudonimo, nitEmpresa, cadena);
        System.out.println("verificaExistenciaSolicitud-secEmpleado: " + secEmpleado);
        System.out.println("verificaExistenciaSolicitud-fechaIniVaca: " + fechaIniVaca);
        /*SimpleDateFormat formato = new SimpleDateFormat("ddMMyyyy");
        String txtFecha = formato.format(fechaIniVaca);*/
        String consulta = "SELECT "
                + "KIOVACACIONES_PKG.VERIFICAEXISTESOLICITUD(?, to_date(?,'DDMMYYYY') ) "
                + "FROM DUAL ";
        System.out.println("verificaExistenciaSolicitud-consulta: " + consulta);
        Query query = null;
        BigDecimal conteo = null;
        try {
            try {
                String esquema = getEsquema(nitEmpresa, cadena);
                setearPerfil(esquema, cadena);
                query = getEntityManager(cadena).createNativeQuery(consulta);
                query.setParameter(1, secEmpleado);
               // query.setParameter(2, txtFecha);
                query.setParameter(2, fechaIniVaca);
            } catch (NullPointerException npe) {
                throw new Exception("verificaExistenciaSolicitud: EntiyManager, query o consulta nulos.");
            }
            Object res = query.getSingleResult();
            System.out.println("verificaExistenciaSolicitud-res: " + res);
            if (res instanceof BigDecimal) {
                conteo = (BigDecimal) res;
                System.out.println("verificaExistenciaSolicitud-conteo: " + conteo);
            } else {
                throw new Exception("El conteo de la solicitud no es BigDecimal. " + res + " tipo: " + res.getClass().getName());
            }
        } catch (Exception e) {
            System.out.println("verificaExistenciaSolicitud-excepcion: " + e);
//            throw e;
            throw new Exception("Error verificando si la solicitud ya existe " + e);
        }
        System.out.println("verificaExistenciaSolicitud-conteo: " + conteo);
        return conteo;
    }
    
    @GET
    @Path("/validaFechaInicioVacaciones")
    @Produces(MediaType.APPLICATION_JSON)
    public String validaFechaInicioSoliciVacaciones(@QueryParam("seudonimo") String seudonimo, @QueryParam("nitempresa") String nitEmpresa, 
            @QueryParam("fechainicio") String fechainicialdisfrute, @QueryParam("cadena") String cadena) {
        String mensaje = "";
        boolean valido = true;
        try { 
            BigDecimal codigoJornada = consultarCodigoJornada(seudonimo, nitEmpresa, fechainicialdisfrute, cadena);
            if (!validaFechaPago(seudonimo, nitEmpresa, fechainicialdisfrute, cadena)) {
                mensaje+= "La fecha seleccionada es inferior a la última fecha de pago.";
                valido = false;
            } else if (verificarFestivo(fechainicialdisfrute, nitEmpresa, cadena)){
                mensaje+= "La fecha seleccionada es un día festivo.";
                valido = false;
            } else if (!verificarDiaLaboral(fechainicialdisfrute,codigoJornada, nitEmpresa, cadena)){
                mensaje+= "La fecha seleccionada no es un día laboral.";
                valido = false;
            }
            
        } catch (Exception ex) {
            Logger.getLogger(EmpleadosFacadeREST.class.getName()).log(Level.SEVERE, null, ex);
            mensaje =  "Ha ocurrido un error, por favor intentalo de nuevo más tarde.";
        }
        JSONObject obj = new JSONObject();
        try {
            obj.put("valido", valido);
            obj.put("mensaje", mensaje);
        } catch (JSONException ex) {
            Logger.getLogger(EmpleadosFacadeREST.class.getName()).log(Level.SEVERE, null, ex);
        }
        return obj.toString();
    }
    
    public String getApellidoNombreXsecEmpl(String secEmpl, String nitEmpresa, String cadena) {
        System.out.println("getApellidoNombreXsecEmpl() secuenciaEmpl: "+secEmpl);
        String nombre = null;
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
        try {
            String sqlQuery = "SELECT UPPER(P.PRIMERAPELLIDO||' '||P.SEGUNDOAPELLIDO||' '||P.NOMBRE) NOMBRE "
                    + " FROM PERSONAS P, EMPLEADOS EMPL "
                    + " WHERE P.SECUENCIA=EMPL.PERSONA "
                    + " AND EMPL.SECUENCIA=?";
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, secEmpl);
            nombre = (String) query.getSingleResult();
            System.out.println("nombre: "+nombre);
        } catch (Exception e) {
            System.out.println("Error "+this.getClass().getName()+".getApellidoNombreXsecEmpl(): " + e);
        }
        return nombre;
    }
    
    @GET
    @Path("/enviaReporteInfoRRHH")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean enviaReporteInfoRRHH(@QueryParam("seudonimo") String seudonimo, @QueryParam("nitempresa") String nitEmpresa, 
            @QueryParam("observacion") String observacionNovedad, @QueryParam("asunto") String asunto, 
            @QueryParam("urlKiosco") String urlKiosco, @QueryParam("grupo") String grupo, @QueryParam("cadena") String cadena) {
        String secEmpl = getSecuenciaEmplPorSeudonimo(seudonimo, nitEmpresa, cadena);
        String nombreEmpl = getApellidoNombreXsecEmpl(secEmpl, nitEmpresa, cadena);
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String mensaje = "Nos permitimos informar que "+nombreEmpl+" ha reportado la siguiente "
                + "observación desde el módulo Kiosco para su validación: "
                + "<br><br>"
                + observacionNovedad;
        String correoUsuario = getCorreoConexioneskioskos(seudonimo, nitEmpresa, cadena);
        System.out.println("Parametros enviaReporteInfoRRHH(): seudonimo "+seudonimo+", nit: "+nitEmpresa+", urlKiosco: "+urlKiosco+", grupo: "+grupo+", cadena: "+cadena);
        boolean enviado = true;
        asunto += " de "+nombreEmpl+" "+fecha;
        try { 
            EnvioCorreo e= new EnvioCorreo();
            //if (e.enviarCorreoInformativo("Módulo Kiosco: Reporte de corrección de información de "+nombreEmpl+" "+fecha,
            if (e.enviarCorreoInformativo(asunto,
                    "Estimado personal de Nómina y RRHH:", mensaje, nitEmpresa, urlKiosco+"#/login/"+grupo, cadena, getCorreoSoporteKiosco(nitEmpresa, cadena), correoUsuario)) {
                enviado = true;
            } else {
                enviado= false;
            }
        } catch (Exception ex) {
            Logger.getLogger(EmpleadosFacadeREST.class.getName()).log(Level.SEVERE, null, ex);
            mensaje =  "Ha ocurrido un error, por favor intentalo de nuevo más tarde.";
        }
        return enviado;
    }
    
    public String getCorreoSoporteKiosco(String nitEmpresa, String cadena) {
        System.out.println("getCorreoSoporteKiosco()");
        List emailSoporte=null;
        String correoDestinatarios="";
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "SELECT EMAILCONTACTO FROM KIOPERSONALIZACIONES WHERE "
                    + "EMPRESA=(SELECT SECUENCIA FROM EMPRESAS WHERE NIT=?)";
            System.out.println("Query: "+sqlQuery);
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, nitEmpresa);
            emailSoporte =  query.getResultList();
                    Iterator<String> it = emailSoporte.iterator();
        //System.out.println("obtener " + correoDestinatarioMain.get(0));
        System.out.println("size: " + emailSoporte.size());
        while (it.hasNext()) {
            String correoenviar = it.next(); 
            System.out.println("correo auditoria: " + correoenviar);
            correoDestinatarios += correoenviar;
            if (it.hasNext()) {
                correoDestinatarios +=",";
            }
            //c.pruebaEnvio2("smtp.gmail.com","587","pruebaskiosco534@gmail.com","Nomina01", "S", correoenviar,
        }
            //emailSoporte =  query.getSingleResult().toString();
            System.out.println("Emails soporte: "+correoDestinatarios);
        } catch (Exception e) {
            System.out.println("Error: getCorreoSoporteKiosco: "+e.getMessage());
        }
        return correoDestinatarios;
    }     
        
    
    public String getCorreoConexioneskioskos(String seudonimo, String nitEmpresa, String cadena) {
        System.out.println("Parametros " + this.getClass().getName() + ".getCorreoConexioneskioskos(): seudonimo: " + seudonimo + ", empresa: " + nitEmpresa + ", cadena: " + cadena);
        String correo = null;
        String sqlQuery;
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            sqlQuery = "SELECT P.EMAIL FROM PERSONAS P, conexioneskioskos ck WHERE p.secuencia=ck.persona and "
                    + " ck.seudonimo=? and ck.nitempresa=?";
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, seudonimo);
            query.setParameter(2, nitEmpresa);
            correo = query.getSingleResult().toString();
        } catch (Exception e) {
            System.out.println("Error " + this.getClass().getName() + ".getCorreoConexioneskioskos(): " + e.getMessage());
        }
        return correo;
    }
    
    @GET
    @Path("/educacionesFormales")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getEducacionesFormales(@QueryParam("usuario") String seudonimo, @QueryParam("empresa") String nitEmpresa, @QueryParam("cadena") String cadena) {
        System.out.println("parametros getEducacionesFormales(): usuario: "+seudonimo+" nitEmpresa: "+nitEmpresa+ " cadena "+cadena);
        List s = null;
        String secEmpl = null;
        try {
            secEmpl = getSecuenciaEmplPorSeudonimo(seudonimo, nitEmpresa, cadena);
        } catch (Exception e) {
            System.out.println("Error al consultar secuencia del empleado");
        }
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "select "
                    + "te.nombre, p.descripcion, i.descripcion institucion, to_char(vf.fechavigencia, 'dd/mm/yyyy') fechaVigencia, "
                    + "to_char(vf.fechavencimiento, 'dd/mm/yyyy') fechaVencimiento, ad.descripcion adiest, vf.NUMEROTARJETA, vf.OBSERVACION, "
                    + "to_char(FECHAEXPEDICIONTARJETA, 'dd/mm/yyyy') fechaExpedicionTarjeta, to_char(FECHAVENCIMIENTOTARJETA, 'dd/mm/yyyy') fechaVencimientoTarjeta "
                    + "from "
                    + "empleados e, personas per, vigenciasformales vf, tiposeducaciones te, profesiones p, instituciones i, adiestramientosf ad "
                    + "where "
                    + "vf.persona=per.secuencia "
                    + "and e.persona=per.secuencia "
                    + "and vf.tipoeducacion=te.secuencia "
                    + "and vf.profesion = p.secuencia "
                    + "and vf.institucion = i.secuencia(+) "
                    + "and ad.secuencia(+)=vf.adiestramientof "
              //      + "and vf.FECHAVIGENCIA>=empleadocurrent_pkg.FechaVigenciaTipoContrato(e.secuencia, sysdate) "
                    + "and e.secuencia=? "
                    + "order by vf.fechavigencia";
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, secEmpl);
            s = query.getResultList();
            s.forEach(System.out::println);
            return Response.status(Response.Status.OK).entity(s).build();
        } catch (Exception ex) {
            System.out.println("Error "+this.getClass().getName()+"getEducacionesFormales(): " + ex);
            try {
                            String sqlQuery = "select "
                    + "te.nombre, p.descripcion, i.descripcion institucion, to_char(vf.fechavigencia, 'dd/mm/yyyy') fechaVigencia, "
                    + "to_char(vf.fechavencimiento, 'dd/mm/yyyy') fechaVencimiento, ad.descripcion adiest, vf.NUMEROTARJETA, vf.OBSERVACION, "
                    + "to_char(FECHAEXPEDICIONTARJETA, 'dd/mm/yyyy') fechaExpedicionTarjeta, to_char(FECHAVENCIMIENTOTARJETA, 'dd/mm/yyyy') fechaVencimientoTarjeta "
                    + "from "
                    + "empleados e, personas per, vigenciasformales vf, tiposeducaciones te, profesiones p, instituciones i, adiestramientosnf ad "
                    + "where "
                    + "vf.persona=per.secuencia "
                    + "and e.persona=per.secuencia "
                    + "and vf.tipoeducacion=te.secuencia "
                    + "and vf.profesion = p.secuencia "
                    + "and vf.institucion = i.secuencia(+) "
                    + "and ad.secuencia(+)=vf.adiestramientof "
              //      + "and vf.FECHAVIGENCIA>=empleadocurrent_pkg.FechaVigenciaTipoContrato(e.secuencia, sysdate) "
                    + "and e.secuencia=? "
                    + "order by vf.fechavigencia";
            } catch (Exception e) {
            }
            return Response.status(Response.Status.NOT_FOUND).entity("Error").build();
        }
    }

    @GET
    @Path("/educacionesNoFormales")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getEducacionesNoFormales(@QueryParam("usuario") String seudonimo, @QueryParam("empresa") String nitEmpresa, @QueryParam("cadena") String cadena) {
        System.out.println("parametros getEducacionesNoFormales(): usuario: "+seudonimo+" nitEmpresa: "+nitEmpresa+ " cadena "+cadena);
        List s = null;
        try {
        String secEmpl = getSecuenciaEmplPorSeudonimo(seudonimo, nitEmpresa, cadena);
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "select "
                    + "c.nombre CURSO, vf.titulo, i.descripcion institucion, to_char(vf.fechavigencia, 'dd/mm/yyyy') fechavigencia, to_char(vf.fechavencimiento, 'dd/mm/yyyy') fechaVencimiento, "
                    + "ad.DESCCRIPCION "
                    + "adiest, vf.OBSERVACION\n"
                    + "from "
                    + "empleados e, personas per, vigenciasnoformales vf, cursos c, instituciones i, adiestramientosnf ad "
                    + "where "
                    + "vf.persona=per.secuencia "
                    + "and e.persona=per.secuencia "
                    + "and vf.curso = c.secuencia "
                    + "and vf.institucion = i.secuencia(+) "
                    + "and ad.secuencia(+)=vf.adiestramientonf "
                    + "and e.secuencia=? "
                    + "order by vf.fechavigencia";
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            query.setParameter(1, secEmpl);
            s = query.getResultList();
            s.forEach(System.out::println);
            return Response.status(Response.Status.OK).entity(s).build();
        } catch (Exception ex) {
            System.out.println("Error "+this.getClass().getName()+"getEducacionesNoFormales(): " + ex);
            return Response.status(Response.Status.NOT_FOUND).entity("Error").build();
        }
    } 
    
    @GET
    @Path("/obtenerAnexosDocumentos/")
    public List obtenerAnexosDocumentos(@QueryParam("empleado") String empleado, @QueryParam("cadena") String cadena, @QueryParam("empresa") String nitEmpresa) {
        System.out.println("Parametros obtenerAnexosDocumentos(): empleado: " + empleado + ", cadena: " + cadena + ", nitEmpresa: " + nitEmpresa);
        File carpeta = null;
        String esquema = null;
        String secEmpresa = null;
        String secEmpleado = null;
        List documentos = new ArrayList();
        String rutaArchivo = getPathDocumentos(nitEmpresa, cadena)/* "E:\\DesignerRHN10\\Basico10\\Reportes\\kiosko\\documentosAnexos"/*"E:\\DesignerRHN10\\Instalacion\\Kiosko\\prueba"/* "E:\\CLIENTES\\CONSTRUCTORA DE MARCAS S.A.S"*/ ;
        try {
            esquema = getEsquema(nitEmpresa, cadena);
            secEmpresa = getSecuenciaPorNitEmpresa(nitEmpresa, cadena);
            secEmpleado = getSecuenciaEmplPorSeudonimo(empleado, nitEmpresa, cadena);
            carpeta = new File(rutaArchivo + "\\" + secEmpresa + "\\" +secEmpleado + "\\");
            System.out.println("carpeta: " + carpeta);
            File[] listado = null;
            listado = carpeta.listFiles(); 
            System.out.println(documentos);
            System.out.println("documentos ");
            System.out.println("Listado: "+ listado);
            int totalArchivos = 0;
            try {
                totalArchivos =  listado.length;
            } catch (Exception e) {
                System.out.println("Error al contar cantidad de archivos : "+e.getMessage());
            }
            if (listado == null || totalArchivos <= 0) {
                System.out.println("No hay elementos dentro de la carpeta actual");
            } else {
                try {
                    System.out.println("Cantidad de archivos: "+listado.length);
                    for (int i = 0; i < listado.length; i++) {
                        System.out.println("i=" + i);
                        System.out.println("nombre con ext " + listado[i]);
                        System.out.println("confima ext " + listado[i].getName() + " " + listado[i].getName().endsWith(".pdf"));
                        if (listado[i].getName().endsWith(".pdf") == true || listado[i].getName().endsWith(".PDF") == true) {
                            System.out.println(listado[i].getName());
                            documentos.add(listado[i].getName());
                            System.out.println(documentos);
                        } else {
                            System.out.println("Elemento no registrado");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error al validar extensiones "+e.getMessage());
                }
                System.out.println("Fin ejecución for");
            }
            System.out.println(documentos);
        } catch (Exception ex) {
            System.out.println("Error: "+this.getClass().getName()+" "+ex.getMessage());
        } 

        return documentos;
    }    
    @GET
    @Path("/decargarAnexo/")
    @Produces({"application/pdf"})
    public Response decargarAnexo(@QueryParam("usuario") String usuario, @QueryParam("cadena") String cadena, 
            @QueryParam("empresa") String nitEmpresa, @QueryParam("anexo") String anexo) {
        System.out.println("Parametros decargarAnexo(): anexo: " + anexo + ", cadena: " + cadena + ", "
                + "nitEmpresa: " + nitEmpresa + " usuario " + usuario);
        FileInputStream fis = null;
        File file = null;
        String esquema = null;
        String secEmpresa = null;
        String secEmpleado = null;
        esquema = getEsquema(nitEmpresa, cadena);
        secEmpresa = getSecuenciaPorNitEmpresa(nitEmpresa, cadena);
        secEmpleado = getSecuenciaEmplPorSeudonimo(usuario, nitEmpresa, cadena);
        String rutaArchivo = getPathDocumentos(nitEmpresa, cadena) + "\\" + secEmpresa + "\\" + secEmpleado + "\\" /* "E:\\CLIENTES\\CONSTRUCTORA DE MARCAS S.A.S"*/;
        System.out.println("ruta para encontrar anexos " + rutaArchivo);
        try {
            fis = new FileInputStream(new File(rutaArchivo + anexo));
            file = new File(rutaArchivo + anexo);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConexionesKioskosFacadeREST.class.getName()).log(Level.SEVERE, "Anexo no encontrada: " + anexo, ex);
            System.getProperty("user.dir");
            System.out.println("Ruta del proyecto: " + this.getClass().getClassLoader().getResource("").getPath());;
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(ConexionesKioskosFacadeREST.class.getName()).log(Level.SEVERE, "Error cerrando fis " + anexo, ex);
            }
        }
        Response.ResponseBuilder responseBuilder = Response.ok((Object) file);
        responseBuilder.header("Content-Disposition", "attachment; filename=\"" + anexo + "\"");
        return responseBuilder.build();
    }
    
    public String getPathDocumentos(String nitEmpresa, String cadena) {
        System.out.println("Parametros getPathReportes(): cadena: " + cadena);
        String rutaFoto = "";
        try {
            String esquema = getEsquema(nitEmpresa, cadena);
            setearPerfil(esquema, cadena);
            String sqlQuery = "SELECT PATHARCHIVO FROM GENERALES WHERE ROWNUM<=1";
            System.out.println("Query: " + sqlQuery);
            Query query = getEntityManager(cadena).createNativeQuery(sqlQuery);
            rutaFoto = query.getSingleResult().toString();
            System.out.println("rutaFotos: " + rutaFoto);
        } catch (Exception e) {
            System.out.println("Error: " + this.getClass().getName() + ".getPathReportes(): " + e.getMessage());
        }
        return rutaFoto;
    }
    
    public String getEsquema( String nitEmpresa, String cadena) {
        System.out.println("Parametros getEsquema(): nitempresa: "+nitEmpresa+", cadena: "+cadena);
        String esquema = null;
        String sqlQuery;
        try {
            sqlQuery = "SELECT ESQUEMA FROM CADENASKIOSKOSAPP WHERE NITEMPRESA=? AND CADENA=?";
            Query query = getEntityManager("wscadenaskioskosPU").createNativeQuery(sqlQuery);
            query.setParameter(1, nitEmpresa);
            query.setParameter(2, cadena);
            esquema = query.getSingleResult().toString();
            System.out.println("Esquema: "+esquema);
        } catch (Exception e) {
            System.out.println("Error "+this.getClass().getName()+".getEsquema(): " + e);
        } 
        return esquema;
    }       
    
}
