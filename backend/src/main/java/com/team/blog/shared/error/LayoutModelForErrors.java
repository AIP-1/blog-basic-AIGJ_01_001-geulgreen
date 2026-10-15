package com.team.blog.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.servlet.ModelAndView;

/**
 * 예외 처리기가 돌려주는 화면(404 등)에는 Spring이 @ControllerAdvice의 @ModelAttribute를 채우지 않아
 * 머리말이 로그인 안 한 모습([로그인][가입하기])으로 나왔다. 일반 화면(@Controller)에 붙는 같은 값을 여기서 채운다.
 * 인자는 없거나 요청 하나인 메서드만 부르고, 하나가 실패해도 오류 화면은 그린다.
 */
@Component
class LayoutModelForErrors {

    /** 일반 화면 컨트롤러처럼 보이는 표본(어느 advice가 적용되는지 고를 때만 씀). */
    @Controller
    static final class Probe {
    }

    private final ApplicationContext context;
    private final AtomicReference<List<ControllerAdviceBean>> advices = new AtomicReference<>();

    LayoutModelForErrors(ApplicationContext context) {
        this.context = context;
    }

    void fill(ModelAndView view, HttpServletRequest request) {
        for (ControllerAdviceBean advice : advices.updateAndGet(a -> a != null ? a : ControllerAdviceBean.findAnnotatedBeans(context))) {
            if (advice.getBeanType() == null || !advice.isApplicableToBeanType(Probe.class)) {
                continue;
            }
            Set<Method> methods = MethodIntrospector.selectMethods(advice.getBeanType(),
                    (ReflectionUtils.MethodFilter) m -> AnnotatedElementUtils.hasAnnotation(m, ModelAttribute.class)
                            && !AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class));
            Object bean = null;
            for (Method method : methods) {
                String name = method.getAnnotation(ModelAttribute.class).value();
                if (name.isEmpty() || view.getModel().containsKey(name) || method.getReturnType() == void.class) {
                    continue;
                }
                Object[] args = new Object[method.getParameterCount()];
                boolean supported = true;
                for (int i = 0; i < args.length; i++) {
                    if (method.getParameterTypes()[i].isInstance(request)) {
                        args[i] = request;
                    } else {
                        supported = false;
                    }
                }
                if (!supported) {
                    continue;
                }
                try {
                    if (bean == null) {
                        bean = advice.resolveBean();
                    }
                    ReflectionUtils.makeAccessible(method);
                    view.addObject(name, method.invoke(bean, args));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // 머리말 값 하나가 없어도 오류 화면은 보여 준다
                }
            }
        }
    }
}
